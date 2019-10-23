package ttq.pipeline

import org.scalatest.FunSuite
import ttq.common._
import ttq.parser.PdaParser
import ttq.tokenizer._

class TheGrammarTests extends FunSuite {
  val parser = new PdaParser[Token, Option[Token]](TheGrammar)

  val AGG = AggregateToken(AggregateRef("TotalSales", "total sales", "cls"))
  val BY = KeywordToken(Keywords.by)
  val CITY = DimensionToken(DimensionRef("City", "city", StringType, "cls"))
  val WHERE = KeywordToken(Keywords.where)
  val PRICE = DimensionToken(DimensionRef("Price", "price", NumberType, "cls"))
  val GT_ = KeywordToken(Keywords.gt)
  val N100 = NumberToken(100)

  test("parse tests") {
    var res = parser.parse(List(AGG), 0)
    assert(res.nonEmpty)
//    res.foreach(println)

    res = parser.parse(List(AGG, BY, CITY), 0)
    assert(res.nonEmpty)
//    res.foreach(r => println(r._2))

    res = parser.parse(List(AGG, BY, CITY, WHERE, PRICE, GT_, N100), 0)
    assert(res.nonEmpty)
//    res.foreach(r => println(r._2))

    res = parser.parse(List(AGG, BY, CITY, WHERE, PRICE, N100), 1)
    assert(res.nonEmpty)
//    res.foreach(r => println(r._2))
  }
}
