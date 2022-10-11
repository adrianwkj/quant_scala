package org.peter.quant

import scala.collection.mutable.ListBuffer
import scala.math._

case class MiddleCenter(first: Stroke, second: Stroke, third: Stroke, extendStroke: List[Stroke]) {

  import MiddleCenter._

  val middleRange = priceIntersection(first, second, third)
  val maxRange = priceDrift(first, second, third)
}

object MiddleCenter {

  def priceIntersection(first: Stroke, second: Stroke, third: Stroke): Option[PriceRange] = {
    val firstRange = first.getPriceRange
    val secondRange = second.getPriceRange
    val thirdRange = third.getPriceRange
    val firstRangesecond = rangeMerge(firstRange, secondRange)
    if (firstRangesecond.isEmpty) None
    else rangeMerge(firstRangesecond.get, thirdRange)
  }

  def priceDrift(first: Stroke, second: Stroke, third: Stroke): Option[PriceRange] = {
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
}