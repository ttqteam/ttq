package ttq.lf

import ttq.common._

case class FactsExpr(factsRef: Option[FactsRef]) {
  override def toString: String = factsRef.map(a => a.toString).getOrElse("?")
}

sealed trait AggregateExpr
case class UserAggregateExpr(aggregate: Option[AggregateRef]) extends AggregateExpr {
  override def toString: String = aggregate.map(a => a.toString).getOrElse("?")
}
//case class SumMeasureExpr(measure: MeasureRef) extends AggregateExpr
//case class CountMeasureExpr(measure: MeasureRef) extends AggregateExpr

sealed trait GroupByExpr
case class GroupByDimensionExpr(dimension: Option[DimensionRef]) extends GroupByExpr {
  override def toString: String = dimension.map(a => a.toString).getOrElse("?")
}
//case object GroupByMonth
//case object GroupByYear

sealed trait FilterExpr {
}

case object FactorExpr extends FilterExpr

case class OrExpr(left: FilterExpr, right: FilterExpr) extends FilterExpr {
  override def toString: String = "(" + left + " OR " + right + ")"
}

case class AndExpr(left: FilterExpr, right: FilterExpr) extends FilterExpr {
  override def toString: String = "(" + left + " AND " + right + ")"
}

case class DimensionEqualsEntityExpr(dimension: Option[DimensionRef], entity: Option[StringEntityRef]) extends FilterExpr {
  override def toString: String =
    "DimEqEntity(" +
    dimension.map(a => a.toString).getOrElse("?") + " = " + entity.map(a => a.toString).getOrElse("?") +
    ")"
}
case class DimensionOpNumberExpr(dimension: Option[DimensionRef], operation: Option[Operation], entity: Option[NumberEntityRef]) extends FilterExpr {
  override def toString: String =
    "DimOpNum(" +
    dimension.map(a => a.toString).getOrElse("?") + " " + operation.getOrElse("?") + " " + entity.map(a => a.toString).getOrElse("?") +
    ")"
}
case class FilterByDateExpr(dimension: Option[DimensionRef], fromDate: Option[DateEntityRef]) extends FilterExpr {
  override def toString: String =
    "DimGtDate(" +
    dimension.map(a => a.toString).getOrElse("?") + " > " + fromDate.map(a => a.toString).getOrElse("?") +
    ")"
}
case class LastMonthExpr(dimension: Option[DimensionRef]) extends FilterExpr {
  override def toString: String =
    "LastMonth(" + dimension.map(a => a.toString).getOrElse("?") + ")"
}
case class BooleanDimensionExpr(dimension: Option[DimensionRef]) extends FilterExpr {
  override def toString: String =
    "BooleanDimension(" + dimension.map(a => a.toString).getOrElse("?") + ")"
}

sealed trait Query {
  def getResultUnits: Option[String]
}

case class FactsQuery(factsExpr: FactsExpr, filter: Option[FilterExpr]) extends Query {
  override def toString: String = factsExpr + filter.map(f => " where " + f.toString).getOrElse("")
  override def getResultUnits: Option[String] = None
}

case class AggregateQuery(agg: AggregateExpr, groupBy: Option[GroupByExpr], filterBy: Option[FilterExpr]) extends Query {
  override def toString: String = agg + groupBy.map(g => " by " + g.toString).getOrElse("") + filterBy.map(f => " where " + f.toString).getOrElse("")
  override def getResultUnits: Option[String] = {
    agg match {
      case UserAggregateExpr(Some(aggregateRef)) => aggregateRef.units
      case _ => None
    }
  }
}
