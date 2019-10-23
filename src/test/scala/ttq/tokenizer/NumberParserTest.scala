package ttq.tokenizer

import org.scalatest.FunSuite

class NumberParserTest  extends FunSuite {
  test("test") {
    assert(NumberParser.parse("123").get == 123L)
    assert(NumberParser.parse("1,23").get == 123L)

    assert(NumberParser.parse("123k").get == 123000L)
    assert(NumberParser.parse("123K").get == 123000L)
    assert(NumberParser.parse("123m").get == 123000000L)
    assert(NumberParser.parse("123M").get == 123000000L)
    assert(NumberParser.parse("123b").get == 123000000000L)
    assert(NumberParser.parse("123B").get == 123000000000L)

    assert(NumberParser.parse("123 k").get == 123000L)
    assert(NumberParser.parse("123  m").get == 123000000L)
    assert(NumberParser.parse("123  b").get == 123000000000L)
  }

}
