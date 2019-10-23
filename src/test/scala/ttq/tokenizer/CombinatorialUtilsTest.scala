package ttq.tokenizer

import org.scalatest.FunSuite
import ttq.tokenizer.CombinatorialUtils._

class CombinatorialUtilsTest extends FunSuite {
  test("testGetAllSeqSubsetsWithLenUpToK") {
    assert(10  == getIndexesOfAllSeqSubsetsWithLenUpToK(4, 4).size)
    assert(4  == getIndexesOfAllSeqSubsetsWithLenUpToK(4, 1).size)
  }

  test("splitToSubsetsWithLenUpToK") {
    assert(5  == splitToSubsetsWithLenUpToK(4, 2).size)
    assert(8  == splitToSubsetsWithLenUpToK(4, 4).size)
  }

  test("generateAllCombinations") {
    val list = List(List("a", "b"), List("c", "d", "k"), List("e", "g"))
    generateAllCombinations(list).foreach(x => println(x))
    assert(12 ==generateAllCombinations(list).size)
  }

  test("generateAllCombinationsWithEmptyList") {
    val list = List(List("a", "b"), List("c", "d", "k"), List())
    generateAllCombinations(list).foreach(x => println(x))
    assert(6 ==generateAllCombinations(list).size)
  }

  test("generateAllCombinationsWithEmptyInput") {
    val list = List.empty
    assert(0 == generateAllCombinations(list).size)
  }

  test ("splitListToSegments") {
    val list = List("a", "a", "b", "c", "d", "a", "e", "a")
    splitListToSegments[String](list, x => x == "a").foreach(x => println(x))
    assert(5 == splitListToSegments[String](list, x => x == "a").size)
    assert(1 == splitListToSegments[String](List("b", "c", "d"), x => x == "a").size)
    assert(1 == splitListToSegments[String](List("a", "a", "a"), x => x == "a").size)
    assert(List() == splitListToSegments[String](List(), x => x == "a"))
  }
}
