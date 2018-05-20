package org.apache.livy.examples

import org.apache.livy.scalaapi.ScalaJobContext

object Pi {
  val NumSamples = 100000000

  def main(ctx: ScalaJobContext) = {
    val count = ctx.sc.parallelize(1 to NumSamples).filter { _ =>
      val x = math.random
      val y = math.random
      x*x + y*y < 1
    }.count()

    4.0 * count / NumSamples
  }
}
