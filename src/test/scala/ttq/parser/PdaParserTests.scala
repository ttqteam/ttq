package ttq.parser

import org.scalatest.FunSuite

class PdaParserTests extends FunSuite {

  case object S extends NonTerminal
  case object AGG extends NonTerminal
  case object GROUP extends NonTerminal

  case object Aggregate extends Terminal
  case object Dimension extends Terminal
  case class Word(s:String) extends Terminal

  sealed trait Token
  case object AggToken extends Token
  case object DimToken extends Token
  case class WordToken(name:String) extends Token

  case class TestQuery(agg: TestAggregateExpr, groupBy: Option[TestGroupByExpr])
  case class TestAggregateExpr(aggregateName: String)
  case class TestGroupByExpr(dimensionName: String)

  object TestGrammar extends Grammar[Token, Option[Token]] {
    val rules = List(
      Rule(S, List(AGG), xs => TestQuery(xs(0).asInstanceOf[TestAggregateExpr], None)),
      Rule(S, List(AGG, GROUP), xs => TestQuery(xs(0).asInstanceOf[TestAggregateExpr], Some(xs(1).asInstanceOf[TestGroupByExpr]))),
      Rule(AGG, List(Aggregate), xs => TestAggregateExpr(xs(0).asInstanceOf[Option[Token]].toString)),
      Rule(GROUP, List(Word("by"), Dimension), xs => TestGroupByExpr(xs(1).asInstanceOf[Option[Token]].toString))
    )

    val start = S

    override def tokenToTerminal(token: Token): Terminal = token match {
      case AggToken => Aggregate
      case DimToken => Dimension
      case WordToken(name) => Word(name)
    }

    override def terminalToLeaf(symbol: Terminal): Option[Token] = symbol match {
      case Aggregate => None
      case Dimension => None
      case Word(s) => Some(WordToken(s))
    }

    override def tokenToLeaf(token: Token): Option[Token] = Some(token)

    override def deleteCost(tokenToDelete: Token): Double = 2.0
    override def insertCost(rule:Rule, symbolToInsert: Terminal): Double = 1.0
    override def replaceCost(rule:Rule, tokenToDelete: Token, symbolToInsert: Terminal): Double = 3.0
  }

  val parser = new PdaParser[Token, Option[Token]](TestGrammar)

  test("parse tests") {
    assert(parser.parse(List(AggToken), 0).nonEmpty)
    assert(parser.parse(List(AggToken, WordToken("by"), DimToken), 0).nonEmpty)
    println(parser.parse(List(AggToken, WordToken("by"), DimToken), 0))

    assert(parser.parse(List(DimToken), 0).isEmpty)
    assert(parser.parse(List(AggToken, DimToken), 0).isEmpty)

    assert(parser.parse(List(DimToken), 3).nonEmpty) // single substitute (substitute distance = 3)
    assert(parser.parse(List(AggToken, DimToken), 1).nonEmpty) // insert "by"

    assert(parser.parse(List(DimToken, AggToken, DimToken), 1).isEmpty) // can't fix with distance 1
    assert(parser.parse(List(DimToken, AggToken, DimToken), 3).nonEmpty) // can fix
    println(parser.parse(List(DimToken, AggToken, DimToken), 2)) // can fix
  }
}
