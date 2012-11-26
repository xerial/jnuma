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

      def toBitString(b:Array[Byte]) = {
        val s = for(i <- 0 until b.length * 8) yield {
          if((b(i/8) & (1 << (i%8))) == 0) "0" else "1"
        }
        s.mkString
      }

      val numCPUs = Runtime.getRuntime.availableProcessors();
      val affinity = (0 until numCPUs).par.map { cpu =>
        Numa.getAffinity()
      }
      debug("affinity: %s", affinity.map(toBitString(_)).mkString(", "))


      val s = (0 until numCPUs).par.map { cpu =>
        Numa.setAffinity((cpu + 1) % numCPUs)
        if(cpu % 2 == 0)
          (0 until Int.MaxValue / 3).foreach { i => }
        Numa.getAffinity()
      }
      debug("affinity after setting: %s", s.map(toBitString(_)).mkString(", "))


    }

    "allocate local buffer" in {
      for(i <- 0 until 3) {
        val local = Numa.allocLocal(1024)
        Numa.free(local)
      }
    }


    "allocate buffer on nodes" in {

      val N = 100000

      def access(b:ByteBuffer) {
        val r = new Random(0)
        var i = 0
        val p = 128
        val buf = new Array[Byte](p)
        while(i < N) {
          b.position(r.nextInt(b.limit / p) * p)
          b.get(buf)
          i += 1
        }
      }
      val bl = ByteBuffer.allocateDirect(8 * 1024 * 1024)
      val bj = ByteBuffer.allocate(8 * 1024 * 1024)
      val b0 = Numa.allocOnNode(8 * 1024 * 1024, 0)
      val b1 = Numa.allocOnNode(8 * 1024 * 1024, 1)
      val bi = Numa.allocInterleaved(8 * 1024 * 1024)


      time("numa random access", repeat=10) {

        block("direct") {
          access(bl)
        }

        block("default") {
          access(bj)
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
