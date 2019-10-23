package ttq.ontology

import org.scalatest.FunSuite
import ttq.dsl.Dimension

class ValidatorUtilsTest extends FunSuite {
  test("testGetInnerDimensionNames") {
    val dimension = new Dimension("test").fromSql("$test0 select $test1 - $test2*$test3 % $$test4 from abc")
    assert(ValidatorUtils.getInnerPropertyNames(dimension.getSql) == List("test0", "test1", "test2", "test3"))
  }
}
