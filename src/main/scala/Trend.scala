package org.peter.quant

import scala.collection.LinearSeq

sealed trait Trend

case class Tendency(trendElements: TrendElement*) extends Trend {

  import Tendency._
  def appended(trendElement: TrendElement) = add(this, trendElement)

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
      val lastTrendElement = t.trendElements.last
      lastTrendElement match {
        case trend: SubLevelTrend =>
          val l = SubLevelTrend(trend.strokes ++ x.strokes:_*)
          Tendency(t.trendElements.init.appended(l):_*)
        case _: MiddleCenter =>
          Tendency(t.trendElements.appended(x):_*)
      }
    case x: MiddleCenter =>
      val lastTrendElement = t.trendElements.last
      lastTrendElement match {
        case _: SubLevelTrend =>
          Tendency(t.trendElements.appended(x):_*)
        case _: MiddleCenter =>
          println("mc can't add mc directly.")
          t
      }
  }
}

case class Consolidation(trendElements: TrendElement*) extends Trend {

  import Consolidation._
  def appended(trendElement: TrendElement) = add(this, trendElement)
}

object Consolidation {

  def apply(beforeSubLevelTrend: SubLevelTrend,
            mc: MiddleCenter
           ) = new Consolidation(beforeSubLevelTrend, mc)

  def apply(mc: MiddleCenter
           ) = new Consolidation(mc)

  def add(c: Consolidation, trendElement: TrendElement): Trend = trendElement match {
    case x: SubLevelTrend =>
      val lastTrendElement = c.trendElements.last
      lastTrendElement match {
        case trend: SubLevelTrend =>
          val l = SubLevelTrend(trend.strokes ++ x.strokes:_*)
          Consolidation(c.trendElements.init.appended(l):_*)
        case _: MiddleCenter =>
          Consolidation(c.trendElements.appended(x):_*)
      }
    case x: MiddleCenter =>
      val lastTrendElement = c.trendElements.last
      lastTrendElement match {
        case _: SubLevelTrend =>
          Tendency(c.trendElements.appended(x):_*)
        case _: MiddleCenter =>
          println("mc can't add mc directly.")
          c
      }
  }

}
