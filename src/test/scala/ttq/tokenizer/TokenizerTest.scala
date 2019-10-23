package ttq.tokenizer

import org.scalatest.FunSuite
import ttq.TestUtils.tokensToTokensWithAlias
import ttq.common.{AggregateRef, Probable}

class TokenizerTest extends FunSuite {

  val BUY_SELL_RATIO = AggregateToken(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))
  val BUY = AggregateToken(AggregateRef("buy", "buy", "orders"))
  val BXX = AggregateToken(AggregateRef("bxx", "bxx", "orders"))
  val SELL = AggregateToken(AggregateRef("sell", "sell", "orders"))
  val ASSET = AggregateToken(AggregateRef("asset", "asset", "orders"))
  val ASSET_BLOCK = AggregateToken(AggregateRef("asset group", "asset group", "orders"))
  val knowledgeBase = new KnowledgeBase(tokensToTokensWithAlias(BUY_SELL_RATIO, BUY, SELL, ASSET, ASSET_BLOCK, BXX))
  val tokenizer = new Tokenizer(knowledgeBase)

  test("testGetReplacementProbability") {
    prettyPrint(BUY_SELL_RATIO, "buy")
    prettyPrint(BUY_SELL_RATIO, "buy/sell ratio")
    prettyPrint(ASSET_BLOCK, "asset")
    prettyPrint(ASSET, "asset")
    prettyPrint(ASSET_BLOCK, "asset block")
    prettyPrint(BUY, "buy SELL ratio")
    prettyPrint(BUY_SELL_RATIO, "buy_sell_ratio")
    prettyPrint(BUY_SELL_RATIO, "buy rate")
    prettyPrint(BUY_SELL_RATIO, "buy/sell")
    prettyPrint(BUY_SELL_RATIO, "buuy selllratio")
  }

  test("findReplacementsOfSegmentToToken") {
    tokenizer.findReplacementsOfSegmentToToken(List(WordToken("buy"), WordToken("SELL"), WordToken("ratio")), 0.5).foreach(x => println(x))
    tokenizer.findReplacementsOfSegmentToToken(List(WordToken("buy/sell ratio")), 0.5).foreach(x => println(x))
  }

  test("findReplacementsOfTwoLetterSegmentToToken") {
    val BUY = AggregateToken(AggregateRef("buy", "buy", "orders"))
    val BXX = AggregateToken(AggregateRef("bxx", "bxx", "orders"))
    val knowledgeBase = new KnowledgeBase(tokensToTokensWithAlias(BUY, BXX))
    val tokenizer = new Tokenizer(knowledgeBase)
    val res = tokenizer.findReplacementsOfSegmentToToken(List(WordToken("bu")), 0.1)
    assert(res.find(_.value.name == "buy").get.probability > res.find(_.value.name == "bxx").get.probability)
  }

  test("test local extension") {
    val token1 = AggregateToken(AggregateRef("NumberType, sql", "xxxxx", "cls"))
    val token2 = AggregateToken(AggregateRef("sql", "xabcde", "cls"))
    val token3 = AggregateToken(AggregateRef("sql", "yabcde qwe", "cls"))
    val token4 = AggregateToken(AggregateRef("sql", "zzzzz", "cls"))
    val token5 = AggregateToken(AggregateRef("sql", "zabcde", "cls"))
    val knowledgeBase = new KnowledgeBase(tokensToTokensWithAlias(token1, token2, token3, token4, token5))
    val tokenizer = new Tokenizer(knowledgeBase)
    val res = tokenizer.parseUserInput(List(WordToken("xxxxx"), WordToken("qwe"), WordToken("zzzzz")), 2)

  }

  private def prettyPrint(token: Token, str: String): Unit = {
    val prob = tokenizer.getReplacementProbability(token.name, str)
    println(s"prob of replacement '$str' with token '$token' is '$prob')}")
  }
}
