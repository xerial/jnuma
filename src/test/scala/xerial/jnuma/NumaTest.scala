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
    "report NUMA info" taggedAs ("report") in {
      val available = Numa.isAvailable
      val numNodes = Numa.numNodes()
      debug("numa is available: " + available)
      debug("num nodes: " + numNodes)
      for (i <- 0 until numNodes) {
        val n = Numa.nodeSize(i)
        val f = Numa.freeSize(i)
        debug("node %d - size:%,d free:%,d", i, n, f)
      }


      val nodes = (0 until numNodes)

      for (n1 <- nodes; n2 <- n1 until numNodes) {
        val d = Numa.distance(n1, n2)
        debug("distance %s - %s: %d", n1, n2, d)
      }

      def toBitString(b: Array[Long]) = {
        val s = for (i <- 0 until Numa.numCPUs()) yield {
          if ((b(i / 64) & (1L << (i % 64))) == 0) "0" else "1"
        }
        s.mkString
      }

      for (node <- nodes) {
        val cpuVector = Numa.nodeToCpus(node)
        debug("node %d -> cpus %s", node, toBitString(cpuVector))
      }


      val numCPUs = Runtime.getRuntime.availableProcessors();
      val affinity = (0 until numCPUs).par.map {
        cpu =>
          Numa.getAffinity()
      }
      debug("affinity: %s", affinity.map(toBitString(_)).mkString(", "))

      val preferred = (0 until numCPUs).par.map {
        cpu =>
          Numa.runOnNode(cpu % numNodes)
          Numa.setPreferred(cpu % numNodes)
          val n = Numa.getPreferredNode
          Numa.runOnAllNodes()
          n
      }
      debug("setting prefererd NUMA nodes: %s", preferred.mkString(", "))


      val s = (0 until numCPUs).par.map {
        cpu =>
          Numa.setAffinity((cpu + 1) % numCPUs)
          if (cpu % 2 == 0)
            (0 until Int.MaxValue / 10).foreach {
              i =>
            }
          Numa.getAffinity()
      }
      debug("affinity after setting: %s", s.map(toBitString(_)).mkString(", "))

      val r = (0 until numCPUs).par.map {
        cpu =>
          Numa.resetAffinity()
          Numa.getAffinity()
      }
      debug("affinity after resetting: %s", r.map(toBitString(_)).mkString(", "))

    }

    "allocate local buffer" in {
      for (i <- 0 until 3) {
        val local = Numa.allocLocal(1024)
        Numa.free(local)
      }
    }


    "allocate buffer on nodes" in {

      val N = 100000

      def access(b: ByteBuffer) {
        val r = new Random(0)
        var i = 0
        val p = 1024
        val buf = new Array[Byte](p)
        while (i < N) {
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


      time("numa random access", repeat = 10) {

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

    def radixSort8(buf: ByteBuffer) = {
      val K = 256
      val N = buf.capacity()

      val pile = Array.ofDim[Int](K)
      // count frequencies
      buf.position(0)
      for (i <- 0 until N)
        pile(buf.get(i) + 128) += 1

      // count cumulates
      for (i <- 1 until K) {
        pile(i) += pile(i - 1)
      }

      def split {
        for (i <- 0 until N) {
          var e = buf.get(i)
          var toContinue = true
          while (toContinue) {
            val p = e + 128
            val pileIndex = pile(p) - 1
            pile(p) -= 1
            if (pileIndex < i)
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


    def radixSort8_local(buf: ByteBuffer) = {
      val K = 256
      val N = buf.capacity() - (2 * 4 * K)

      val countOffset = buf.capacity() / 4
      val pileOffset = countOffset + K

      // count frequencies
      buf.position(0)
      for (i <- 0 until K) {
        buf.putInt(countOffset + i * 4, 0)
      }
      for (i <- 0 until buf.capacity()) {
        val ch = buf.get(i) + 128
        val prevCount = buf.getInt(countOffset + ch * 4)
        buf.putInt(countOffset + ch * 4, prevCount + 1)
      }
      // count cumulates
      for (i <- 0 until K) {
        val prev = if (i == 0) 0 else buf.getInt(countOffset + (i - 1) * 4)
        val current = buf.getInt(countOffset + i * 4)
        buf.putInt(pileOffset + i * 4, prev + current)
      }

      def split {
        for (i <- 0 until N) {
          var e = buf.get(i)
          var toContinue = true
          while (toContinue) {
            val p = e + 128
            val pileIndex = buf.getInt(pileOffset + p * 4) - 1
            buf.putInt(pileOffset + p * 4, pileIndex)
            if (pileIndex < i)
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



    def radixSort8_array(b: Array[Byte]) = {
      val K = 256
      val N = b.length
      val count = new Array[Int](K + 1)

      // count frequencies
      for (i <- 0 until N) {
        count(b(i) + 128) += 1
      }
      // count cumulates
      for (i <- 1 to K)
        count(i) += count(i - 1)

      def split {
        val pile = new Array[Int](K + 1)
        Array.copy(count, 1, pile, 0, K)
        for (i <- 0 until N) {
          var e = b(i)
          var toContinue = true
          while (toContinue) {
            val p = e + 128
            val pileIndex = pile(p) - 1
            pile(p) -= 1
            if (pileIndex < i)
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


    "perform microbenchmark" taggedAs ("bench") in {

      val bufferSize = 4 * 1024 * 1024
      when("buffer size is %,d".format(bufferSize))

      val numaBufs = (for (i <- 0 until Numa.numNodes()) yield "numa%d".format(i) -> Numa.allocOnNode(bufferSize, i)) :+
        "numa-i" -> Numa.allocInterleaved(bufferSize)


      //val bdirect = ByteBuffer.allocateDirect(bufferSize)
      val bheap = ByteBuffer.allocate(bufferSize).order(ByteOrder.nativeOrder());

      val bufs = numaBufs ++ Map("heap" -> bheap)

      // fill bytes
      def fillBytes(b: ByteBuffer) = {
        var i = 0
        b.clear()
        while (b.remaining() > 0) {
          b.put(i.toByte)
          i += 1
        }
      }

      def fillInt(b: ByteBuffer) = {
        val r = new Random(0)
        b.clear()
        var i = 0
        while (b.remaining() >= 4) {
          b.putInt(r.nextInt())
          i += 1
        }
      }

      val devNull = new FileOutputStream("/dev/null")
      def dump(b: ByteBuffer) = {
        b.position(0)
        b.limit(b.capacity())
        devNull.getChannel().write(b)
      }

      def randomAccess(b: ByteBuffer) = {
        val r = new Random(0)
        val pageSize = 128
        val maxPage = b.capacity() / pageSize
        var i = 0
        val buf = new Array[Byte](pageSize)
        while (i < 100000) {
          b.position(r.nextInt(maxPage) * pageSize)
          b.get(buf)
          i += 1
        }
      }



      def bench[U](name: String, f: ByteBuffer => U, rep: Int = 3) {
        time(name, repeat = rep) {
          for ((name, b) <- bufs)
            block(name) {
              f(b)
            }
        }
      }

      bench("fill byte", fillBytes)
      bench("fill int", fillInt)
      bench("dump to /dev/null", dump)
      bench("random page read", randomAccess)

      val R = 1
      bench("radix sort", radixSort8, rep = R)
      val ba = Array.ofDim[Byte](bufferSize)
      val r = new Random(0)
      for (i <- 0 until bufferSize / 4) {
        val v = r.nextInt
        ba(i) = ((v >> 24) & 0xFF).toByte
        ba(i + 1) = ((v >> 16) & 0xFF).toByte
        ba(i + 2) = ((v >> 8) & 0xFF).toByte
        ba(i + 3) = (v & 0xFF).toByte
      }

      time("radix sort on java array", repeat = R) {
        radixSort8_array(ba)
      }


      for ((name, b) <- numaBufs)
        Numa.free(b)
    }

    def boundTo[U](cpu: Int)(f: => U): U = {
      try {
        Numa.setAffinity(cpu)
        f
      }
      finally
        Numa.resetAffinity
    }

    "sort in parallel" taggedAs ("psort") in {

      val bufferSize = 1024 * 1024

      def init(b: ByteBuffer) {
        val r = new Random(0)
        for (i <- 0 until bufferSize) {
          b.put(i, r.nextInt.toByte)
        }
      }
      val N = 1


      val holder = Seq.newBuilder[ByteBuffer]
      val C = Numa.numCPUs
      debug("start parallel sorting using %d CPUs", C)
      time("sorting", repeat = 3) {
        block("numa-aware", repeat = N) {
          val M = Numa.numNodes
          (0 until C).par.foreach {
            cpu =>
              boundTo(cpu) {
                val node = cpu % M;
                Numa.runOnNode(node)
                Numa.setPreferred(node)
                val buf = Numa.allocOnNode(bufferSize, node)
                holder += buf
                init(buf)
                radixSort8(buf)
                Numa.runOnAllNodes();
                Numa.setLocalAlloc();
              }
          }
        }

        //        block("numa-aware-local", repeat=N) {
        //          val M = Numa.numNodes
        //          (0 until C).par.foreach { cpu =>
        //            boundTo(cpu) {
        //              val buf = Numa.allocOnNode(bufferSize, cpu % M)
        //              holder += buf
        //              init(buf)
        //              radixSort8_local(buf)
        //            }
        //          }
        //        }

        block("heap", repeat = N) {
          (0 until C).par.foreach {
            cpu =>
              boundTo(cpu) {
                val buf = ByteBuffer.allocate(bufferSize)
                init(buf)
                radixSort8(buf)
              }
          }
        }

        block("wrapped", repeat = N) {
          (0 until C).par.foreach {
            cpu =>
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

    "allocate more than 2GB array" taggedAs("long") in {
       val N = 4L * 1024L * 1024L * 1024L
       for(i <- 0 until 10) {
	   debug("alloc")
           val addr = Numa.allocMemory(N)
	   debug("free")
	   Numa.free(addr, N)
       }	
    }


    "allocate memory and a CPU to a specific node" taggedAs ("pref") in {

      val numCPU = Numa.numCPUs
      val bufferSize = 1024 * 1024
      def newArray = {
        val a = Array.ofDim[Int](bufferSize)
        val r = new Random(0)
        for (i <- 0 until bufferSize)
          a(i) = r.nextInt
        a
      }

      debug("start Array sorting")
      val R = 2
      val RR = 10
      time("alloc", repeat = R) {

        block("numa-aware", repeat=RR) {
          val M = Numa.numNodes
            (0 until numCPU).par.foreach {
              cpu =>
                  boundTo(cpu) {
                    val node = cpu % M
                    Numa.runOnNode(node)
                    Numa.setPreferred(node)
                    val a = newArray
                    util.Arrays.sort(a)
                    Numa.runOnAllNodes()
                    Numa.setLocalAlloc()
                  }
            }
        }

        block("numa-anti", repeat=RR) {
          val M = Numa.numNodes
          (0 until numCPU).par.foreach {
            cpu =>
              boundTo(cpu) {
                val node = (cpu + 1) % M
                Numa.setPreferred(node)
                val a = newArray
                util.Arrays.sort(a)
                Numa.setLocalAlloc()
              }
          }
        }

        block("default", repeat=RR) {
          (0 until numCPU).par.foreach {
            cpu =>
                boundTo(cpu) {
                  val a = newArray
                  util.Arrays.sort(a)
                  Numa.runOnAllNodes()
                  Numa.setLocalAlloc()
                }
          }
        }


      }
    }

    "retrieve memory from another nodes" taggedAs("remote") in {

      val prop = System.getProperty("jnuma.test.bf", "8192")
      debug("buffer size: %s", prop)
      val B = prop.toInt
      def access(b:ByteBuffer) {
        b.position(0)
        for(i <- 0 until B) {
          b.get
        }
      }

      def alloc(f: => ByteBuffer) {
        val r = new Random(13)
        val b = f
        b.position(0)
        for(i <- 0 until B / 4)
          b.putInt(r.nextInt())
//          b.position(0)
//          for(i <- 0 until B / 4)
//            b.getInt
        radixSort8(b)
        Numa.free(b)
      }

      Numa.runOnNode(0)
      debug("using a cpu on node %d", 0)
      time("numa") {
        block("node0") {
          alloc(Numa.allocOnNode(B, 0))
        }

        block("node1") {
          alloc(Numa.allocOnNode(B, 1))
        }

        block("iv") {
          alloc(Numa.allocInterleaved(B))
        }
      }

      Numa.runOnAllNodes()
    }

    "retrieve array from another node" taggedAs("jarray") in {

      val B = 1024 * 1024

      def alloc(f: => Array[Int]) {
        val r = new Random(13)
        for(i <- 0 until 1000) {
          val arr = f
          for(index <- 0 until B / 4)
            arr(index) = r.nextInt()
        }
      }

      Numa.runOnNode(0)
      debug("using a cpu on node %d", 0)
      time("numa", repeat=2) {
        block("node0") {
          debug("allocate array on node 0")
          Numa.setPreferred(0)
          alloc{
            val a = new Array[Int](B)
            Numa.toNodeMemory(a, B * 4, 0)
            a
          }
          Numa.setLocalAlloc
        }

        block("node1") {
          debug("allocate array on node 1")
          Numa.setPreferred(1)
          alloc{
            val a = new Array[Int](B)
            Numa.toNodeMemory(a, B * 4, 1)
            a
          }
          Numa.setLocalAlloc
        }
      }

      Numa.runOnAllNodes()
    }

    "bulk parallel write" taggedAs("pwrite") in {

      val C = Numa.numCPUs

      val bufferSize = 1024 * 1024 * 1024
      val WR = 1

      def writeBench(cpu:Int, b:ByteBuffer) {
        (0 until WR).foreach { r =>
          for(i <- 0 until bufferSize / 256) {
            b.put(i, Random.nextInt(256).toByte)
            if(cpu == 0 && i > 0 && (i % (1024 * 1024)) == 0) {
              debug("wrote at %,d bytes", i)
            }
          }
        }
      }

      time("pwrite", repeat=2) {
        block("heap") {
          debug("heap write")
          (0 until C).par.foreach { cpu =>
            boundTo(cpu) {
              val b = ByteBuffer.allocate(bufferSize)
              writeBench(cpu, b)
            }
          }
        }

        block("numa") {
          debug("numa local write")
          (0 until C).par.foreach { cpu =>
            boundTo(cpu) {
              val b = Numa.allocLocal(bufferSize)
              writeBench(cpu, b)
              Numa.free(b)
            }
          }
        }
      }


    }

  }
}
