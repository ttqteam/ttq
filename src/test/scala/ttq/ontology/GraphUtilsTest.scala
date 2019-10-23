package ttq.ontology

import org.scalatest.FunSuite

class GraphUtilsTest extends FunSuite {
  test("topSort") {
    val graph = Map("a" -> List("b", "c"), "b" -> List("d"), "c" -> List("d"), "d" -> List("e"), "e" -> List())
    print(new GraphUtils().topSort(graph))
  }

  test("topSort1") {
    val graph = Map("a" -> List("b", "c"), "b" -> List("c"), "c" -> List("d"), "d" -> List())
    print(new GraphUtils().topSort(graph))
  }

  test("topSortError") {
    val graph = Map("a" -> List("b", "c"), "b" -> List("d"), "c" -> List("d"), "d" -> List("e"), "e" -> List("b"))
    intercept[IllegalStateException] {
      new GraphUtils().topSort(graph)
    }
  }
}
