//--------------------------------------
//
// NumaTest.scala
// Since: 2012/11/22 2:24 PM
//
//--------------------------------------

package xerial.jnuma

import util.Random
import java.nio.ByteBuffer


/**
 * @author leo
 */
class NumaTest extends MySpec {

  "Numa" should {
    "allocate local buffer" in {
      for(i <- 0 until 3) {
        val local = Numa.allocLocal(1024)
        Numa.free(local)
      }
    }


    "allocate buffer on nodes" in {

      val N = 10000000

      def access(b:ByteBuffer) {
        val r = new Random(0)
        var i = 0
        while(i < N) {
          b.get(r.nextInt(b.limit))
          i += 1
        }
      }

      time("numa random access", repeat=100) {
        block("0") {
          val b = Numa.allocOnNode(4 * 1024 * 1024, 0)
          access(b)
          Numa.free(b)
        }

        block("1") {
          val b = Numa.allocOnNode(4 * 1024 * 1024, 1)
          access(b)
          Numa.free(b)
        }

      }

    }


  }
}
