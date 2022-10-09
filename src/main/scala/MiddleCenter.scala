package org.peter.quant

import scala.math._

case class MiddleCenter(one: Stroke, two: Stroke, three: Stroke) {

  import MiddleCenter._

  val middleRange = priceIntersection(one, two, three)
  val maxRange = priceDrift(one, two, three)
}

object MiddleCenter {

  def priceIntersection(one: Stroke, two: Stroke, three: Stroke): Option[PriceRange] = {
    val oneRange = one.getPriceRange
    val twoRange = two.getPriceRange
    val threeRange = three.getPriceRange
    val oneRangetwo = rangeMerge(oneRange, twoRange)
    if (oneRangetwo.isEmpty) None
    else rangeMerge(oneRangetwo.get, threeRange)
  }

  def priceDrift(one: Stroke, two: Stroke, three: Stroke): Option[PriceRange] = {
    val oneRange = one.getPriceRange
    val twoRange = two.getPriceRange
    val threeRange = three.getPriceRange
    val oneRangetwo = rangeUnion(oneRange, twoRange)
    if (oneRangetwo.isEmpty) None
    else rangeUnion(oneRangetwo.get, threeRange)
  }

  def prepareCheck(one: Stroke, two: Stroke, three: Stroke): Boolean = {
    if (one.endPoint == two.startPoint && two.endPoint == three.startPoint) true
    else false
  }

  private def rangeMerge(one: PriceRange, two: PriceRange): Option[PriceRange] = {
    if (one.lowPrice <= two.highPrice && one.highPrice >= two.lowPrice) {
      Some(PriceRange(max(one.lowPrice, two.lowPrice), min(one.highPrice, two.highPrice)))
    } else None
  }

  private def rangeUnion(one: PriceRange, two: PriceRange): Option[PriceRange] = {
    if (one.lowPrice <= two.highPrice && one.highPrice >= two.lowPrice) {
      Some(PriceRange(min(one.lowPrice, two.lowPrice), max(one.highPrice, two.highPrice)))
    } else None
  }
}