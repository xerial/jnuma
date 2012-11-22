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

      val N = 10000

      def access(b:ByteBuffer) {
        val r = new Random(0)
        var i = 0
        val p = 8 * 1024
        val buf = new Array[Byte](p)
        while(i < N) {
          b.position(r.nextInt(b.limit / p) * p)
          b.get(buf)
          i += 1
        }
      }
      val bl = ByteBuffer.allocateDirect(8 * 1024 * 1024)
      val b0 = Numa.allocOnNode(8 * 1024 * 1024, 0)
      val b1 = Numa.allocOnNode(8 * 1024 * 1024, 1)
      val b2 = Numa.allocOnNode(8 * 1024 * 1024, 2)


      time("numa random access", repeat=100) {

        block("default") {
          access(bl)
        }

        block("0") {
          access(b0)
        }

        block("1") {
          access(b1)
        }
        block("2") {
          access(b2)
        }
      }

      Numa.free(b0)
      Numa.free(b1)
    }


  }
}
