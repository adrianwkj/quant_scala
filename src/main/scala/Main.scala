package org.peter.quant

import scala.language.postfixOps
import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp
import java.text.SimpleDateFormat
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._


object Main {

  val db = Database.forConfig("mydb")

  val symbol = "000729"

  val circle = "Day"

  val start_time_str = "2021-10-10"
  val end_time_str = "2022-10-10"

  //  val dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS")
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val start_time = new Timestamp(dateFormat.parse(start_time_str).getTime)
  val end_time = new Timestamp(dateFormat.parse(end_time_str).getTime)

  val stocks = TableQuery[Stocks]
    .filter(_.symbol === symbol)
    .filter(x => x.trade_date >= start_time && x.trade_date <= end_time)
    .sortBy(_.id)

  val resultFuture = db.run(stocks.result)

  def main(args: Array[String]) = {


    import Stock._
    val raw = Await.result(resultFuture, Duration.Inf)
    println(raw.length)
    val mergedRawData = raw.map(List(_)).reduce {
      (x, y) => {
        merge(x.last, y.last) match {
          case z if z._1 == null => x.dropRight(1) ++ List(z._2)
          case z if z._2 == null => x.dropRight(1) ++ List(z._1)
          case z => x.dropRight(1) ++ List(z._1, z._2)
        }
      }
    }

    println("mergedRawData: " + mergedRawData.length)

    val stockList = ListBuffer[org.peter.quant.Stocks#TableElementType]()
    for (i <- 1 until mergedRawData.length - 1) {
      val before = mergedRawData(i - 1)
      val after = mergedRawData(i + 1)
      val bc = compare(before, mergedRawData(i))
      val ac = compare(mergedRawData(i), after)
      val tempShape: org.peter.quant.shape = (bc, ac) match {
        case (`down`, `up`) => bottom
        case (`up`, `down`) => top
        case _ => relay
      }
      //
      //      println("current stock: " + y(i) + " shape: " + tempShape + " i:" + i)
      if (stockList.nonEmpty) {
        if (tempShape == relay) {
          mergedRawData(i).shape = tempShape
          //          println("== relay: " + y(i))
        } else {
          val lastShapeStock = stockList.last
          //          println("last shaped: " + lastShapeStock)
          val tempIndex = mergedRawData.indexOf(lastShapeStock)
          if (i - tempIndex < 4) {
            mergedRawData(i).shape = relay
            //            println("< 4: " + y(i))
          } else {
            if (tempShape != lastShapeStock.shape) {
              mergedRawData(i).shape = tempShape
              //              println("insert: " + y(i))
              stockList.addOne(mergedRawData(i))
            } else {
              if ((tempShape == top && lastShapeStock.high <= mergedRawData(i).high) || (tempShape == bottom && lastShapeStock.low >= mergedRawData(i).low)) {
                //                println("before remove: " + stockList.last)
                stockList -= lastShapeStock
                mergedRawData(i).shape = tempShape
                stockList.addOne(mergedRawData(i))
                //                println("after insert: " + stockList.last)
              } else {
                mergedRawData(i).shape = relay
                //                println("----: "+ y(i))
              }
            }
          }
        }
      } else {
        if (tempShape != relay) {
          mergedRawData(i).shape = tempShape
          stockList += mergedRawData(i)
          //          println("First: " + y(i))
        }
      }
    }

    println("stockList length: " + stockList.length)

    val pointList = stockList.map {
      x =>
        if (x.shape == top) Point(x.id, x.trade_date, x.high)
        else if (x.shape == bottom) Point(x.id, x.trade_date, x.low)
        else null
    }

    val strokeList = ListBuffer[Stroke]()
    for (i <- 1 until pointList.length) {
      strokeList.addOne(Stroke(pointList(i - 1), pointList(i)))
    }
    println("strokeList length: " + strokeList.length)

//    strokeList.foreach(x => println(x + x.direction.toString))

    val strokes = TableQuery[Strokes]
    val strokesDeleteAction = strokes.filter(_.symbol === symbol).filter(x => x.trade_date >= start_time && x.trade_date <= end_time).delete
    val toBeInsertedStrokes = strokeList.map {
      x => StrokeData(0, symbol, circle, x.startPoint.trade_datetime, x.startPoint.price)
    }.map {
      row => strokes.insertOrUpdate(row)
    }
    val strokesInsertAction = DBIO.sequence(toBeInsertedStrokes.toSeq)
    val strokesDbioFuture = db.run(strokesDeleteAction andThen (strokesInsertAction))
    val strokesRowsAffected = Await.result(strokesDbioFuture, Duration.Inf).sum
    println("strokes inserted: " + strokesRowsAffected)

    val mcList = identifyMC(strokeList.toList, List[MiddleCenter]())

    //    val mcList = firstMC(strokeList.toList)
    val middleCenters = TableQuery[MiddleCenters]
    val mcDeleteAction = middleCenters.filter(_.symbol === symbol).filter(x => x.start_time >= start_time && x.end_time <= end_time).delete
    val toBeInsertedMCs = mcList.map {
      mc =>
        MCData(0, symbol, circle, mc.timeRange.startTime, mc.timeRange.endTime)
    }.map {
      row => middleCenters.insertOrUpdate(row)
    }

    val mcInsertAction = DBIO.sequence(toBeInsertedMCs)
    val mcDbioFuture = db.run(mcDeleteAction andThen (mcInsertAction))
    val mcRowsAffected = Await.result(mcDbioFuture, Duration.Inf).sum
    println("middle centers inserted: " + mcRowsAffected)

    val bidAskPoints = mcList.map {
      mc =>
        if (mc.extendStroke.isEmpty) {
          val index = strokeList.indexOf(mc.third)
          val s = strokeList(index + 1)
          if(s.direction == up) (s.endPoint, "v")
          else (s.endPoint, "^")
        } else {
          val index = strokeList.indexOf(mc.extendStroke.last)
          val s = strokeList(index + 1)
          if(s.direction == up) (s.endPoint, "v")
          else (s.endPoint, "^")
        }
    }


    val points = TableQuery[Points]
    val pointsDeleteAction = points.filter(_.symbol === symbol).filter(x => x.trade_time >= start_time && x.trade_time <= end_time).delete
    val toBeInsertedPoints = bidAskPoints.map {
      point =>
        PointData(0, symbol, circle, point._1.trade_datetime, point._1.price, point._2)
    }.map {
      row => points.insertOrUpdate(row)
    }
    val pointsInsertAction = DBIO.sequence(toBeInsertedPoints)
    val pointsDBIOFuture = db.run(pointsDeleteAction andThen(pointsInsertAction))
    val pointsRowsAffected = Await.result(pointsDBIOFuture, Duration.Inf).sum
    println("bid ask points inserted: " + pointsRowsAffected)


  }

  //  def permute[T](xs: List[T], ys: List[T]) = {
  //    for {x <- xs; y <- ys} yield (x,y)
  //  }


  def identifyMC(strokeList: List[Stroke], mcList: List[MiddleCenter]): List[MiddleCenter] = {

    if (strokeList.length < 3) {
      mcList
    } else {

      if (mcList.isEmpty) {

        val mc = firstMC(strokeList)
        if (mc.isDefined) {
          val thirdStroke = mc.get.third
          val index = strokeList.indexOf(thirdStroke)
          identifyMC(strokeList.drop(index + 1), List(mc.get))
        } else {
          List[MiddleCenter]()
        }
      } else {

        val lastMC = mcList.last
        val currentStroke = strokeList.head
        if (currentStroke.getPriceRange.ifOverlap(lastMC.middleRange.get)) {
          val extendStroke = lastMC.extendStroke.appended(currentStroke)
          val newMC = lastMC.copy(extendStroke = extendStroke)
          val tempMCList = mcList.dropRight(1).appended(newMC)
          identifyMC(strokeList.drop(1), tempMCList)
        } else {

          val mc = MiddleCenter(strokeList.head, strokeList(1), strokeList(2), List[Stroke]())
          val middleRange = mc.middleRange

          if (middleRange.isDefined) {
            mc.joinBefore(lastMC) match {
              case (x, y) if x.isDefined && y.isDefined =>
                val newMCList = mcList.appended(mc) // add a new mc
                identifyMC(strokeList.drop(3), newMCList)
              case (x, y) if x.isEmpty && y.isDefined =>
                val newMCList = mcList.init.appended(mc) //drop the last mc and add the new one
                identifyMC(strokeList.drop(3), newMCList)
              case (x, y) if x.isDefined && y.isEmpty =>
                identifyMC(strokeList.drop(1), mcList)
              case (x, y) if x.isEmpty && y.isEmpty =>
                identifyMC(strokeList.drop(1), List[MiddleCenter]())
            }

          } else {
            identifyMC(strokeList.drop(2), mcList)
          }
        }
      }
    }
  }


  @tailrec
  def firstMC(strokeList: List[Stroke]): Option[MiddleCenter] = {
    if (strokeList.length >= 3) {
      val mc = MiddleCenter(strokeList.head, strokeList(1), strokeList(2), List[Stroke]())
      val middleRange = mc.middleRange

      if (middleRange.isDefined) {
        Some(mc)
      } else {
        firstMC(strokeList.drop(1))
      }
    } else {
      None
    }
  }



  def regenerateConsolidation(strokes: List[Stroke]) = {

  }
}

