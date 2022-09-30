package org.peter.quant

import java.util.Date

case class CandleStick(date: Date, open: Float, close: Float, high: Float, low: Float) {
  val list = List(open, close, high, low)

  def get_highest() = list.max

  def get_lowest() = list.min


}

object CandleStick {
  def compare(before: CandleStick, after: CandleStick): compare_result = (before, after) match {
    case _ if(before.get_highest() > after.get_highest() && before.get_lowest() > after.get_lowest()) =>
      down
    case _ if(before.get_highest() > after.get_highest() && before.get_lowest() < after.get_lowest()) =>
      included
    case _ if(before.get_highest() < after.get_highest() && before.get_lowest() > after.get_lowest()) =>
      include
    case _ if(before.get_highest() < after.get_highest() && before.get_lowest() < after.get_lowest()) =>
      up
  }

  def merge(before: CandleStick, after: CandleStick) =
    compare(before, after) match {
      case included => before
      case _ => after
    }
}


sealed trait compare_result
case object up extends compare_result
case object down extends compare_result
case object include extends compare_result
case object included extends compare_result