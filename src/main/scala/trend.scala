package org.peter.quant

import scala.collection.LinearSeq

sealed trait Trend[T <: TrendElement] extends LinearSeq[T]

case class Tendency(trendElement: TrendElement*) extends Trend[TrendElement] {

}

object Tendency {

  def apply(beforeSubLevelTrend: SubLevelTrend,
            firstMC: MiddleCenter,
            middleSubLevelTrend: SubLevelTrend,
            secondMC: MiddleCenter) = new Tendency(beforeSubLevelTrend, firstMC, middleSubLevelTrend, secondMC)

  def apply(firstMC: MiddleCenter,
            middleSubLevelTrend: SubLevelTrend,
            secondMC: MiddleCenter) = new Tendency(firstMC, middleSubLevelTrend, secondMC)

  private def add(t: Tendency, trendElement: TrendElement): Tendency = trendElement match {
    case x: SubLevelTrend =>
      val lastTrendElement = t.last
      lastTrendElement match {
        case trend: SubLevelTrend[TrendElement] =>
          val l = trend ++ x

          t.init.appended(l)
        case _: MiddleCenter =>
          Tendency(t.trendElementList.appended(x))
      }
    case x: MiddleCenter =>
      val lastTrendElement = t.trendElementList.last
      lastTrendElement match {
        case trend: SubLevelTrend =>
          Tendency(t.trendElementList.appended(x))
        case _: MiddleCenter =>
          println("mc can't add mc directly.")
          t
      }
  }
}

case class Consolidation(trendElementList: List[TrendElement]) extends Trend

object Consolidation {

  def apply(beforeSubLevelTrend: SubLevelTrend,
            firstMC: MiddleCenter,
            middleSubLevelTrend: SubLevelTrend,
            secondMC: MiddleCenter) = new Consolidation(List(beforeSubLevelTrend, firstMC, middleSubLevelTrend, secondMC))

}

trait Person[T <: TrendElement] extends LinearSeq[T]

class Mi(a: String*) extends Person[TrendElement]

object Mi {
  val zxy = new Mi("abc", "bcd")

}