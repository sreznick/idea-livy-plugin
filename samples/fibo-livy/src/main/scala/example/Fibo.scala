package org.apache.livy.examples

import org.apache.livy.scalaapi.ScalaJobContext

object Fibo {
  def fibo(n: Int): Int = {
    def fibo(curr: Int, a:Int, b: Int): Int = {
        if (curr == n) a else fibo(curr + 1, a + b, a)
    }

    if (n == 0) 1 else fibo(1, 1, 1)
  }

  def main(ctx: ScalaJobContext) = {
    List(fibo(0), fibo(1), fibo(2), fibo(3), fibo(10))
  }
}
