package ttq.tokenizer

import org.scalatest.FunSuite
import ttq.TestUtils.tokensToTokensWithAlias
import ttq.common.AggregateRef

class TokenizerTest2 extends FunSuite {
  test("single word") {
    val token = AggregateToken(AggregateRef("sql", "xxxxx", "cls"))
    val knowledgeBase = new KnowledgeBase(tokensToTokensWithAlias(token))
    val fastTokenizer = new Tokenizer(knowledgeBase)
    assert(fastTokenizer.parseUserInput(List(WordToken("xxxxx"))).size == 1)
  }

  test("fruitless permutations ignored") {
    // tests that (xxxxx yyyyy) (zzzzz) and (xxxxx) (yyyyy zzzzz) do not generate duplicate (xxxxx) (zzzzz)
    val token1 = AggregateToken(AggregateRef("sql", "xxxxx", "cls"))
    val token3 = AggregateToken(AggregateRef("sql", "zzzzz", "cls"))
    val knowledgeBase = new KnowledgeBase(tokensToTokensWithAlias(token1, token3))

    val tokenizer = new Tokenizer(knowledgeBase)
    val res1 = tokenizer.parseUserInput(List(WordToken("xxxxx"), WordToken("yyyyy"), WordToken("zzzzz")))
    assert(res1.size == 1)
  }

  test("many unknown words") {
    val token1 = AggregateToken(AggregateRef("sql", "xxxxx", "cls"))
    val knowledgeBase = new KnowledgeBase(tokensToTokensWithAlias(token1))
    val tokenizer = new Tokenizer(knowledgeBase)
    assert(tokenizer.parseUserInput(List(WordToken("xxxxx"), WordToken("yyyyy"), WordToken("zzzzz"), WordToken("wwwww"))).size == 1)
  }

  test("not failing when can't find tokens") {
    val knowledgeBase = new KnowledgeBase(List())
    val tokenizer = new Tokenizer(knowledgeBase)
    val res1 = tokenizer.parseUserInput(List(WordToken("xxx")))
    assert(res1.size == 0)
  }
}
