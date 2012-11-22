//--------------------------------------
//
// NumaTest.scala
// Since: 2012/11/22 2:24 PM
//
//--------------------------------------

package xerial.jnuma

import util.Random
import java.nio.ByteBuffer
import java.util


/**
 * @author leo
 */
class NumaTest extends MySpec {

  "Numa" should {
    "repot NUMA info" in {
      val available = Numa.isAvailable
      val maxNodes = Numa.maxNodes()
      debug("numa is available: " + available)
      debug("max nodes: " + maxNodes)
      for(i <- 0 to maxNodes) {
        val n = Numa.nodeSize(i)
        val f = Numa.freeSize(i)
        debug("node %d - size:%,d free:%,d", i, n, f)
      }

      val cpu = Numa.currentCpu()
      debug("current CPU: %d", cpu)

      val nodes = (0 to maxNodes)

      for(n1 <- nodes; n2 <- n1 to maxNodes) {
        val d = Numa.distance(n1, n2)
        debug("distance %s - %s: %d", n1, n2, d)
      }

      for(node <- nodes) {
        val cpuVector = Numa.nodeToCpus(node)
        def vecStr = {
          val b = for(i <- 0 until Runtime.getRuntime.availableProcessors()) yield {
            if((cpuVector(i / 64) & (0x01 << (i % 64))) == 0) "0" else "1"
          }
          b.mkString
        }
        debug("node %d -> cpus %s", node, vecStr)
      }
    }

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
        val p = 4 * 1024
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
      val bi = Numa.allocInterleaved(8 * 1024 * 1024)

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
        block("interleaved") {
          access(bi)
        }
      }

      Numa.free(b0)
      Numa.free(b1)
      Numa.free(bi)
    }


  }
}
