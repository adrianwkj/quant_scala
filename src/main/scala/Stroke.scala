package org.peter.quant

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._

case class Stroke(startPoint: Point, endPoint: Point) {
  def getPriceRange: PriceRange = Stroke.priceRange(startPoint, endPoint)

  val direction: direction = (startPoint.price, endPoint.price) match {
    case (m, n) if (m > n) => down
    case (m, n) if (m < n) => up
    case _ => level
  }
}

object Stroke {

  private def priceRange(startPoint: Point, endPoint: Point) = {
    if (startPoint.price <= endPoint.price) PriceRange(startPoint.price, endPoint.price)
    else PriceRange(endPoint.price, startPoint.price)
  }
}

//sealed trait point
case class Point(stockId: Int, trade_datetime: Timestamp, price: Float)

case class PriceRange(lowPrice: Float, highPrice: Float) {

  def ifOverlap(pr: PriceRange): Boolean = {
    if(lowPrice <= pr.highPrice && highPrice >= pr.lowPrice) true
    else false
  }
}

object PriceRange {

  def apply(lowPrice: Float, highPrice: Float): PriceRange = {
    if (lowPrice > highPrice) {
      null
    } else {
      new PriceRange(lowPrice, highPrice)
    }
  }
}


case class StrokeData(id: Int, symbol: String, circle: String, trade_date: Timestamp, price: Float)

class Strokes(tag: Tag) extends Table[StrokeData](tag, "stroke") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def symbol = column[String]("symbol")

  def circle = column[String]("circle")

  def trade_date = column[Timestamp]("trade_date")

  def price = column[Float]("price")

  def * = (id, symbol, circle, trade_date, price) <> ((StrokeData.apply _).tupled, StrokeData.unapply)
}
