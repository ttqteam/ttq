package ttq.execution

import ttq.common._
import ttq.execution.readers.{ScalarReader, BarChartResultReader, TableResultReader}
import ttq.lf._
import ttq.ontology.FinalOntology

class QueryToResultReaderConverter(ontology: FinalOntology) {
  def queryToResultReader(query: Query): ResultReader = query match {
    case q: AggregateQuery => q.groupBy match {
      case None => ScalarReader(getUnits(q.agg))
      case Some(groupByExpr) => BarChartResultReader(getDataType(groupByExpr), Seq(getSeriesInfo(q.agg)))
    }

    case q: FactsQuery =>
      val cls = ontology.getClass(q.factsExpr.factsRef.get.className)
      val columns = cls
        .visibleDimensions
        .map(d => SeriesInfo(d.name, d.dataType, None))
      TableResultReader(columns)
  }

  private def getSeriesInfo(aggregateExpr: AggregateExpr): SeriesInfo = aggregateExpr match {
    case UserAggregateExpr(aggregate) =>
      aggregate match {
        case Some(a) => SeriesInfo(a.name, NumberType, a.units)
        case None => throw new IllegalStateException("AggregateExpr is not complete")
      }
  }

  private def getUnits(aggregateExpr: AggregateExpr): Option[String] = aggregateExpr match {
    case UserAggregateExpr(aggregate) =>
      aggregate match {
        case Some(a) => a.units
        case None => throw new IllegalStateException("AggregateExpr is not complete")
      }
  }

  private def getDataType(groupByExpr: GroupByExpr): DataType = groupByExpr match {
    case GroupByDimensionExpr(groupBy) =>
      groupBy match {
        case Some(g) => g.dataType
        case None => throw new IllegalStateException("GroupByExpr is not complete")
      }
  }
}
