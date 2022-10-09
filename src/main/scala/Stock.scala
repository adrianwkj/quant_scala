package org.peter.quant

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._

case class Stock(id: Int,
                 symbol: String,
                 trade_date: Timestamp,
                 open: Float,
                 close: Float,
                 high: Float,
                 low: Float,
                 volume: Int,
                 turnover: Float,
                 amplitude: Float,
                 change_percent: Float,
                 change: Float,
                 turnover_rate: Float) {
//  val list = List(open, close, high, low)

  var direction:direction = null

  var shape:shape = null
}

object Stock {
def compare(before: Stock, after: Stock): direction = (before, after) match {
    case _ if (before.high > after.high && before.low > after.low) =>
      down
    case _ if (before.high.>=(after.high) && before.low.<=(after.low)) =>
      included
    case _ if (before.high.<=(after.high) && before.low.>=(after.low)) =>
      include
    case _ if (before.high < after.high && before.low < after.low) =>
      up
    case _ => null
  }

  def merge(before: Stock, current: Stock) = {
    compare(before, current) match {
      case `included` =>
        if (before.direction == up){
          val newStock = before.copy(low = current.low)
          (newStock, null)
        } else if (before.direction == down) {
          val newStock = before.copy(high = current.high)
          (newStock, null)
        } else {
          (before, null)
        }
      case `include` =>
        if (before.direction == up){
          val newStock = current.copy(low = before.low)
          newStock.direction = up
          (null, newStock)
        } else if (before.direction == down) {
          val newStock = current.copy(high = before.high)
          newStock.direction = down
          (null, newStock)
        } else {
          (null, current)
        }
      case `up` =>
        before.direction = up
        current.direction = up
        (before, current)
      case `down` =>
        before.direction = down
        current.direction = down
        (before, current)
    }
  }
}

sealed trait shape
case object top extends shape
case object bottom extends shape
case object relay extends shape

sealed trait direction
case object up extends direction
case object down extends direction
case object include extends direction
case object included extends direction
case object level extends direction


class Stocks(tag: Tag) extends Table[Stock](tag, "stock_hist") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def symbol = column[String]("symbol")
  def trade_date = column[Timestamp]("trade_date")
  def open = column[Float]("open")
  def close = column[Float]("close")
  def high = column[Float]("high")
  def low = column[Float]("low")
  def volume = column[Int]("volume")
  def turnover = column[Float]("turnover")
  def amplitude = column[Float]("amplitude")
  def change_percent = column[Float]("change_percent")
  def change = column[Float]("change")
  def turnover_rate = column[Float]("turnover_rate")
  def * = (id, symbol, trade_date, open, close, high, low, volume, turnover, amplitude, change_percent, change, turnover_rate) <> ((Stock.apply _).tupled, Stock.unapply)
}
