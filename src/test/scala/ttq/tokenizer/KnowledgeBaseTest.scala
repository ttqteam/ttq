package ttq.tokenizer

import org.scalatest.FunSuite
import ttq.common.{DimensionRef, StringType}

class KnowledgeBaseTest extends FunSuite {
  val token1 = TokenWithAlias(DimensionToken(DimensionRef("abcde", "abcde", StringType, "class")), "abcde")
  val token2 = TokenWithAlias(DimensionToken(DimensionRef("bcdbcd", "bcdbcd", StringType, "class")), "bcdbcd")
  val token3 = TokenWithAlias(DimensionToken(DimensionRef("a", "a", StringType, "class")), "a")
  val knowledgeBase = new KnowledgeBase(List(token1, token2, token3))

  test("testNgramsToTokens") {
    assert(knowledgeBase.getTokensByNgram("a#") == Set(token1, token3))
    assert(knowledgeBase.getTokensByNgram("b#") == Set(token2))
    assert(knowledgeBase.getTokensByNgram("abc") == Set(token1))
    assert(knowledgeBase.getTokensByNgram("bcd") == Set(token1, token2))
    assert(knowledgeBase.getTokensByNgram("cde") == Set(token1))
    assert(knowledgeBase.getTokensByNgram("a") == Set(token3))
    assert(knowledgeBase.getTokensByNgram("^abc") == Set(token1))
    assert(knowledgeBase.getTokensByNgram("^bcd") == Set(token2))
    assert(knowledgeBase.getTokensByNgram("^a") == Set(token3))
    assert(knowledgeBase.getTokensByNgram("cdb") == Set(token2))
    assert(knowledgeBase.getTokensByNgram("dbc") == Set(token2))
    assert(knowledgeBase.getTokensByNgram("cde$") == Set(token1))
    assert(knowledgeBase.getTokensByNgram("bcd$") == Set(token2))
    assert(knowledgeBase.getTokensByNgram("a$") == Set(token3))
  }
}
