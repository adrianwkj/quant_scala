package org.peter.quant

import java.text.SimpleDateFormat

object Main {

  @main def run() =
    val dd = new SimpleDateFormat("yyyyMMdd").parse("20220930")
    val a = CandleStick(dd, 2, 2, 3, 7)
    val b = CandleStick(dd, 1, 2.1, 3, 6)
    import CandleStick._
    val c = compare(a, b)
    val d = List(1,2,3,4).reduceRight{
      (x,y) => {
        x - y
      }
    }

    println(d)


}
