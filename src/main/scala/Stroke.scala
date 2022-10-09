package org.peter.quant

import java.sql.Timestamp

case class Stroke(startPoint: Point, endPoint: Point) {
  def getPriceRange: PriceRange = Stroke.priceRange(startPoint, endPoint)

  val direction: direction = (startPoint.price, endPoint.price) match {
    case (m, n) if(m > n) => down
    case (m, n) if(m < n) => up
    case _ => level
  }
}

object Stroke {

  private def priceRange(startPoint: Point, endPoint: Point) = {
    if(startPoint.price <= endPoint.price) PriceRange(startPoint.price, endPoint.price)
    else PriceRange(endPoint.price, startPoint.price)
  }
}

//sealed trait point
case class Point(trade_datetime: Timestamp, price: Float)

case class PriceRange(lowPrice: Float, highPrice: Float)

object PriceRange {

  def apply(lowPrice: Float, highPrice: Float): PriceRange = {
    if(lowPrice > highPrice) {
      null
    } else {
      new PriceRange(lowPrice, highPrice)
    }
  }
}