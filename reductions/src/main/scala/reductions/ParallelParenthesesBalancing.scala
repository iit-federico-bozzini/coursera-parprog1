package reductions

import scala.annotation._
import org.scalameter._
import common._

object ParallelParenthesesBalancingRunner {

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 120,
    Key.verbose -> true
  ) withWarmer (new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime ms")

    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime ms")
    println(s"speedup: ${seqtime / fjtime}")
  }
}

object ParallelParenthesesBalancing {

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
    */
  def balance(chars: Array[Char]): Boolean = {
    def balanceAux(i: Int, openParentheses: Int): Boolean =
      if (i >= chars.length)
        openParentheses == 0
      else
        chars(i) match {
          case '(' => balanceAux(i + 1, openParentheses + 1)
          case ')' => if (openParentheses == 0) false else balanceAux(i + 1, openParentheses - 1)
          case _ => balanceAux(i + 1, openParentheses)
        }

    balanceAux(0, 0)
  }

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
    */
  def parBalance(chars: Array[Char], threshold: Int=1): Boolean = {

    def traverse(idx: Int, until: Int, closePar: Int, openPar: Int): (Int, Int) = {
      if (idx >= until)
        (closePar, openPar)
      else
        chars(idx) match {
          case '(' => traverse(idx + 1, until, closePar, openPar+1)
          case ')' => if (openPar == 0) traverse(idx + 1, until, closePar+1, openPar) else traverse(idx + 1, until, closePar, openPar - 1)
          case _ => traverse(idx + 1, until, closePar, openPar)
        }
    }

    def reduce(from: Int, until: Int): (Int, Int) = {
      if (until - from > threshold) {
        val middle = (until + from) / 2
        val (l, r) = parallel(reduce(from, middle), reduce(middle, until))
        (l._1, l._2-r._1+r._2)
      }
      else {
        traverse(from, until, 0, 0)
      }
    }

    reduce(0, chars.length) == (0,0)
  }

  // For those who want more:
  // Prove that your reduction operator is associative!

}
