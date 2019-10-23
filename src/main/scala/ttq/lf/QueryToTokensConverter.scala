package ttq.lf

import com.typesafe.scalalogging.Logger
import ttq.common._
import ttq.pipeline.Keywords
import ttq.tokenizer._

object QueryToTokensConverter {
  private val logger = Logger[QueryToTokensConverter.type]

  def convert(query: Query): List[Token] = query match {
    case query:AggregateQuery => aggTokens(query.agg) ++ query.groupBy.map(groupTokens).getOrElse(Nil) ++ query.filterBy.map(whereTokens).getOrElse(Nil)
    case query:FactsQuery => factsTokens(query.factsExpr) ++ query.filter.map(whereTokens).getOrElse(Nil)
  }

  private def factsTokens(factsExpr: FactsExpr): List[Token] = {
    factsExpr match {
      case FactsExpr(Some(ref)) => List(FactsToken(ref))
      case _ => ???
    }
  }

  private def aggTokens(aggregateExpr: AggregateExpr): List[Token] = {
    aggregateExpr match {
      case UserAggregateExpr(Some(ref)) => List(AggregateToken(ref))
      case _ => ???
    }
  }

  private def groupTokens(groupByExpr: GroupByExpr): List[Token] = {
    groupByExpr match {
      case GroupByDimensionExpr(Some(ref)) => List(KeywordToken(Keywords.by), DimensionToken(ref))
      case _ => ???
    }
  }

  private def whereTokens(filterExpr: FilterExpr): List[Token] = {
    List(KeywordToken(Keywords.where)) ++ bexprTokens(filterExpr)
  }

  private def bexprTokens(filterExpr: FilterExpr): List[Token] = {
    def opToString(op:Operation) = op match {
      case EQ => "="
      case LT => "<"
      case GT => ">"
    }

    def wrapInParensIfOrExpr(expr: FilterExpr): List[Token] = {
      expr match {
        case orExpr: OrExpr => List(KeywordToken(Keywords.lparen)) ++ bexprTokens(orExpr) ++ List(KeywordToken(Keywords.rparen))
        case anyOtherExpr => bexprTokens(anyOtherExpr)
      }
    }

    // todo: change this mess somehow
    filterExpr match {
      case AndExpr(left, right) => wrapInParensIfOrExpr(left) ++ List(KeywordToken(Keywords.and)) ++ wrapInParensIfOrExpr(right)
      case OrExpr(left, right) => bexprTokens(left) ++ List(KeywordToken(Keywords.or)) ++ bexprTokens(right)
      case DimensionEqualsEntityExpr(Some(dimRef), Some(entRef)) =>
        List(DimensionToken(dimRef), KeywordToken("="), entityRefToToken(entRef))
      case DimensionOpNumberExpr(Some(dimRef), Some(op), Some(entRef)) =>
        List(DimensionToken(dimRef), KeywordToken(opToString(op)), entityRefToToken(entRef))
      case FilterByDateExpr(Some(dimRef), Some(fromDate)) =>
        // todo: nicer text for date
        List(DimensionToken(dimRef), KeywordToken(">"), DateToken(fromDate.value))
      case LastMonthExpr(Some(dimRef)) =>
        List(DimensionToken(dimRef), KeywordToken(Keywords.lastMonth))
      case BooleanDimensionExpr(Some(dimRef)) =>
        List(DimensionToken(dimRef))
      case _ => ??? // todo
    }
  }

  private def entityRefToToken(ref: EntityRef) = ref match {
    case StringEntityRef(dimension, value) => StringEntityToken(dimension, value)
    case NumberEntityRef(value) => NumberToken(value)
    case DateEntityRef(value) => DateToken(value)
  }
}
