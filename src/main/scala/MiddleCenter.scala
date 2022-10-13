package org.peter.quant

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._
import scala.math._

case class MiddleCenter(first: Stroke, second: Stroke, third: Stroke, extendStroke: List[Stroke]) {

  import MiddleCenter._

  val middleRange = priceIntersection(first, second, third)
  val maxRange = priceDrift(first, second, third)
  val timeRange = timeIntersection(first, third, extendStroke)

  def joinBefore(mc: MiddleCenter) = join(mc, this)
}

object MiddleCenter {

  private def priceIntersection(first: Stroke, second: Stroke, third: Stroke): Option[PriceRange] = {
    val firstRange = first.getPriceRange
    val secondRange = second.getPriceRange
    val thirdRange = third.getPriceRange
    val firstRangeSecond = rangeMerge(firstRange, secondRange)
    if (firstRangeSecond.isEmpty) None
    else rangeMerge(firstRangeSecond.get, thirdRange)
  }

  private def priceDrift(first: Stroke, second: Stroke, third: Stroke): Option[PriceRange] = {
    val firstRange = first.getPriceRange
    val secondRange = second.getPriceRange
    val thirdRange = third.getPriceRange
    val firstRangeSecond = rangeUnion(firstRange, secondRange)
    if (firstRangeSecond.isEmpty) None
    else rangeUnion(firstRangeSecond.get, thirdRange)
  }

  def prepareCheck(first: Stroke, second: Stroke, third: Stroke): Boolean = {
    if (first.endPoint == second.startPoint && second.endPoint == third.startPoint) true
    else false
  }

  private def rangeMerge(first: PriceRange, second: PriceRange): Option[PriceRange] = {
    if (first.lowPrice <= second.highPrice && first.highPrice >= second.lowPrice) {
      Some(PriceRange(max(first.lowPrice, second.lowPrice), min(first.highPrice, second.highPrice)))
    } else None
  }

  private def rangeUnion(first: PriceRange, second: PriceRange): Option[PriceRange] = {
    if (first.lowPrice <= second.highPrice && first.highPrice >= second.lowPrice) {
      Some(PriceRange(min(first.lowPrice, second.lowPrice), max(first.highPrice, second.highPrice)))
    } else None
  }

  private def timeIntersection(first: Stroke, third: Stroke, extendStroke: List[Stroke]): TimeRange = {
    val start_time = first.startPoint.trade_datetime
    if (extendStroke.isEmpty) {
      val end_time = third.endPoint.trade_datetime
      TimeRange(start_time, end_time)
    } else {
      val end_time = extendStroke.last.endPoint.trade_datetime
      TimeRange(start_time, end_time)
    }
  }

  def join(before: MiddleCenter, after: MiddleCenter): (Option[MiddleCenter], Option[MiddleCenter]) = {
    if(after.middleRange.get.mean > before.middleRange.get.mean) {
      if(after.first.direction == down) {
        if(before.first.direction == after.first.direction) {
          (Some(before), Some(after))
        } else {
          (None, Some(after))
        }
      } else {
        if(before.first.direction == down) (Some(before), None)
        else (None, None)
      }

    } else {
      if(after.first.direction == up) {
        if(before.first.direction == after.first.direction) {
          (Some(before), Some(after))
        } else {
          (None, Some(after))
        }
      } else {
        if(before.first.direction == up) (Some(before), None)
        else (None, None)
      }
    }
  }
}

case class TimeRange(startTime: Timestamp, endTime: Timestamp)

case class MCData(id: Int, symbol: String, circle: String, start_time: Timestamp, end_time: Timestamp)

class MiddleCenters(tag: Tag) extends Table[MCData](tag, "middlecenter") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def symbol = column[String]("symbol")

  def circle = column[String]("circle")

  def start_time = column[Timestamp]("start_time")

  def end_time = column[Timestamp]("end_time")

  def * = (id, symbol, circle, start_time, end_time) <> ((MCData.apply _).tupled, MCData.unapply)
}
