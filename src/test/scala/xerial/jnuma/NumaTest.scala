/*
 * Copyright 2012 Taro L. Saito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//--------------------------------------
//
// NumaTest.scala
// Since: 2012/11/22 2:24 PM
//
//--------------------------------------

package xerial.jnuma

import util.Random
import java.nio.{ByteOrder, ByteBuffer}
import java.util
import java.io.{OutputStream, FileOutputStream}


/**
 * @author leo
 */
class NumaTest extends MySpec {

  "Numa" should {
    "repot NUMA info" in {
      val available = Numa.isAvailable
      val numNodes = Numa.numNodes()
      debug("numa is available: " + available)
      debug("num nodes: " + numNodes)
      for(i <- 0 until numNodes) {
        val n = Numa.nodeSize(i)
        val f = Numa.freeSize(i)
        debug("node %d - size:%,d free:%,d", i, n, f)
      }


      val nodes = (0 until numNodes)

      for(n1 <- nodes; n2 <- n1 until numNodes) {
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

      def toBitString(b:Array[Long]) = {
        val s = for(i <- 0 until Numa.numCPUs()) yield {
          if((b(i/64) & (1L << (i%64))) == 0) "0" else "1"
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
          (0 until Int.MaxValue / 10).foreach { i => }
        Numa.getAffinity()
      }
      debug("affinity after setting: %s", s.map(toBitString(_)).mkString(", "))

      val r = (0 until numCPUs).par.map { cpu =>
        Numa.resetAffinity()
        Numa.getAffinity()
      }
      debug("affinity after resetting: %s", r.map(toBitString(_)).mkString(", "))

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
        val p = 1024
        val buf = new Array[Byte](p)
        while(i < N) {
          b.position(r.nextInt(b.capacity() / p) * p)
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

        block("heap") {
          access(bj)
        }

        block("numa0") {
          access(b0)
        }

        block("numa1") {
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

    def radixSort8(buf:ByteBuffer) = {
      val K = 256
      val N = buf.capacity()

      val pile = Array.ofDim[Int](K)
      // count frequencies
      buf.position(0)
      for(i <- 0 until N)
        pile(buf.get(i) + 128) += 1

      // count cumulates
      for(i <- 1 until K) {
        pile(i) += pile(i-1)
      }

      def split {
        for(i <- 0 until N) {
          var e = buf.get(i)
          var toContinue = true
          while(toContinue) {
            val p = e + 128
            val pileIndex = pile(p) - 1
            pile(p) -= 1
            if(pileIndex < i)
              toContinue = false
            else {
              val tmp = buf.get(pileIndex)
              buf.put(pileIndex, e)
              e = tmp
            }
          }
          buf.put(i, e)
        }
      }
      split
    }


    def radixSort8_array(b:Array[Byte]) = {
      val K = 256
      val N = b.length
      val count = new Array[Int](K+1)

      // count frequencies
      for(i <- 0 until N) {
        count(b(i) + 128) += 1
      }
      // count cumulates
      for(i <- 1 to K)
        count(i) += count(i-1)

      def split {
        val pile = new Array[Int](K+1)
        Array.copy(count, 1, pile, 0, K)
        for(i <- 0 until N) {
          var e = b(i)
          var toContinue = true
          while(toContinue) {
            val p = e + 128
            val pileIndex = pile(p) - 1
            pile(p) -= 1
            if(pileIndex < i)
              toContinue = false
            else {
              val tmp = b(pileIndex)
              b(pileIndex) = e
              e = tmp
            }
          }
          b(i) = e
        }
      }
      split
    }


    "perform microbenchmark" taggedAs("bench") in {

      val bufferSize = 4 * 1024 * 1024
      when("buffer size is %,d".format(bufferSize))

      val numaBufs = (for(i <- 0 until Numa.numNodes()) yield "numa%d".format(i) -> Numa.allocOnNode(bufferSize, i)) :+
        "numa-i" -> Numa.allocInterleaved(bufferSize)


      //val bdirect = ByteBuffer.allocateDirect(bufferSize)
      val bheap = ByteBuffer.allocate(bufferSize).order(ByteOrder.nativeOrder());

      val bufs = numaBufs ++ Map("heap" -> bheap)

      // fill bytes
      def fillBytes(b:ByteBuffer) = {
        var i = 0
        b.clear()
        while(b.remaining() > 0) {
          b.put(i.toByte)
          i += 1
        }
      }

      def fillInt(b:ByteBuffer) = {
        val r = new Random(0)
        b.clear()
        var i = 0
        while(b.remaining() >= 4) {
          b.putInt(r.nextInt())
          i += 1
        }
      }

      val devNull = new FileOutputStream("/dev/null")
      def dump(b:ByteBuffer) = {
        b.position(0)
        b.limit(b.capacity())
        devNull.getChannel().write(b)
      }

      def randomAccess(b:ByteBuffer) = {
        val r = new Random(0)
        val pageSize = 128
        val maxPage = b.capacity() / pageSize
        var i = 0
        val buf = new Array[Byte](pageSize)
        while(i < 100000) {
          b.position(r.nextInt(maxPage) * pageSize)
          b.get(buf)
          i += 1
        }
      }



      def bench[U](name:String, f:ByteBuffer => U, rep:Int = 3) {
        time(name, repeat=rep) {
          for((name, b) <- bufs)
            block(name) { f(b) }
        }
      }

      bench("fill byte", fillBytes)
      bench("fill int", fillInt)
      bench("dump to /dev/null", dump)
      bench("random page read", randomAccess)

      val R = 1
      bench("radix sort", radixSort8, rep=R)
      val ba = Array.ofDim[Byte](bufferSize)
      val r = new Random(0)
      for(i <- 0 until bufferSize / 4) {
        val v = r.nextInt
        ba(i) = ((v >> 24) & 0xFF).toByte
        ba(i+1) = ((v >> 16) & 0xFF).toByte
        ba(i+2) = ((v >> 8) & 0xFF).toByte
        ba(i+3) = (v & 0xFF).toByte
      }

      time("radix sort on java array", repeat = R) {
        radixSort8_array(ba)
      }


      for((name, b) <- numaBufs)
        Numa.free(b)
    }

    "sort in parallel" taggedAs("psort") in {

      val bufferSize = 8 * 1024 * 1024

      def init(b:ByteBuffer) {
        val r = new Random(0)
        for(i <- 0 until bufferSize) {
          b.put(i, r.nextInt.toByte)
        }
      }
      val N = 1

      def boundTo[U](cpu:Int)(f: => U): U = {
        try {
          Numa.setAffinity(cpu)
          f 
        }
        finally
          Numa.resetAffinity
      }

      val holder = Seq.newBuilder[ByteBuffer]
      val C = Numa.numCPUs
      debug("start parallel sorting using %d CPUs", C)
      time("sorting", repeat=3) {
        block("numa-aware", repeat=N) {
          val M = Numa.numNodes
          (0 until C).par.foreach { cpu =>
            boundTo(cpu) {
              val buf = Numa.allocOnNode(bufferSize, cpu % M)
              holder += buf
              init(buf)
              radixSort8(buf)
            }
          }
        }

        block("heap", repeat=N) {
          (0 until C).par.foreach { cpu =>
            boundTo(cpu) {
              val buf = ByteBuffer.allocate(bufferSize)
              init(buf)
              radixSort8(buf)
            }
          }
        }

        block("wrapped", repeat=N) {
          (0 until C).par.foreach { cpu =>
            boundTo(cpu) {
              val arr = new Array[Byte](bufferSize)
              val buf = ByteBuffer.wrap(arr)
              init(buf)
              radixSort8(buf)
            }
          }
        }


      }

      holder.result.foreach(b => Numa.free(b))
    }
  }
}
