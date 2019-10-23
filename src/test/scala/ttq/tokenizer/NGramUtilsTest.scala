package ttq.tokenizer

import org.scalatest.FunSuite

class NGramUtilsTest extends FunSuite {
  test("testTokenToNgrams") {
    assert(
      Set(
        "one", "two", "thr", "hre", "ree", "^one", "^on",
        "o#", "t#", "t#", "o t", "t t", "ree$"
      ) == NGramUtils.stringToNgrams("one two three").toSet
    )
    assert(Set() == NGramUtils.stringToNgrams("").toSet)
  }
}
