package ttq.execution

import ttq.lf._
import ttq.ontology.FinalOntology

class QueryToPlanConverter(ontology: FinalOntology) {
  def queryToPlan(query: Query): Plan = {
    val (select, project) = queryToOperators(query)
    Plan(select, project)
  }

  private def queryToOperators(q: Query): (Select, Project) = q match {
    case AggregateQuery(aggExpr, groupByExpr, filterExpr) => (
      Select(getTableName(aggExpr), filterExpr),
      AggregateProject(aggExpr, groupByExpr))
    case FactsQuery(factsExpr, filterExpr) => (
      Select(factsExpr.factsRef.get.sql, filterExpr, factsExpr.factsRef.get.orderBy, Some(500)),
      DimensionProject(ontology.getClass(factsExpr.factsRef.get.className).visibleDimensions))
  }

  private def getTableName(aggregateExpr: AggregateExpr): String = {
    aggregateExpr match {
      case userAggregateExpr: UserAggregateExpr =>
        ontology.getClass(userAggregateExpr.aggregate.get.className).sql
    }
  }
}
