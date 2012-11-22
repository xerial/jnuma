//--------------------------------------
//
// NumaTest.scala
// Since: 2012/11/22 2:24 PM
//
//--------------------------------------

package xerial.jnuma


/**
 * @author leo
 */
class NumaTest extends MySpec {

  "Numa" should {
    "allocate local buffer" in {
      val b = Numa.numaAlloc(1024)
    }
  }
}
