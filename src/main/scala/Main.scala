package org.peter.quant

import scala.language.postfixOps
import slick.jdbc.PostgresProfile.api._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success


object Main {

  val db = Database.forConfig("mydb")

  val symbol = "601668"

  val circle = "Day"

  val stocks = TableQuery[Stocks].filter(_.symbol === symbol)

  val resultFuture = db.run(stocks.result)

  def main(args: Array[String]) = {


    import Stock._
    val raw = Await.result(resultFuture, Duration.Inf)
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
      val tempShape: shape = (bc, ac) match {
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

    val pointList = stockList.map{
      x =>
        if(x.shape == top) Point(x.id, x.trade_date, x.high)
        else if (x.shape == bottom) Point(x.id, x.trade_date, x.low)
        else null
    }

    val strokeList = ListBuffer[Stroke]()
    for(i <- 1 until pointList.length) {
      strokeList.addOne(Stroke(pointList(i - 1), pointList(i)))
    }
    println("strokeList length: " + strokeList.length)

    strokeList.foreach(x => println(x + x.direction.toString))

    val strokes = TableQuery[Strokes]
    val toBeInsertedStrokes = strokeList.map{
      x => StrokeData(0, symbol, circle, x.startPoint.trade_datetime, x.startPoint.price)
    }.map {
      row => strokes.insertOrUpdate(row)
    }
    val dbioFuture = db.run(DBIO.sequence(toBeInsertedStrokes.toSeq))
    val rowsInserted = Await.result(dbioFuture, Duration.Inf).sum
    println(rowsInserted)



//    for(i <- 3 until strokeList.length) {
//      val middleCenter = MiddleCenter(strokeList(i - 2), strokeList(i - 1), strokeList(i))
//      val middleRange = middleCenter.middleRange
//      if(middleRange.isDefined) println(middleCenter)
//    }
  }


}

