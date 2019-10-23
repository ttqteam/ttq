package ttq.lf

import org.scalatest.FunSuite
import ttq.common.{AggregateRef, StringEntityRef}
import ttq.execution.{PlanToSqlConverter, QueryToPlanConverter}

class QueryToSqlConverterTest extends FunSuite {
  private val queryToPlanConverter = new QueryToPlanConverter(TestOntology.ontology)

  private def convertToSql(query: Query) = {
    val plan = queryToPlanConverter.queryToPlan(query)
    PlanToSqlConverter.planToSql(plan, Seq())//todo: add tests with modifiers
  }

  test("testQueryToSql") {
    var query = AggregateQuery(UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))), None, None)
    println(convertToSql(query))

    val dimRef = TestOntology.ASSET_DIM
    val cityRef = TestOntology.CITY_DIM
    query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(
        OrExpr(
          DimensionEqualsEntityExpr(Some(dimRef), Some(StringEntityRef(dimRef, "X"))),
          AndExpr(
            DimensionEqualsEntityExpr(Some(dimRef), Some(StringEntityRef(dimRef, "Y"))),
            DimensionEqualsEntityExpr(Some(cityRef), Some(StringEntityRef(cityRef, "Z"))),
          )
      ))
    )
    println(convertToSql(query))

    query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      Option(GroupByDimensionExpr(Some(dimRef))),
      Option(DimensionEqualsEntityExpr(Some(dimRef), Some(StringEntityRef(dimRef, "EURUSD"))))
    )
    println(convertToSql(query))
  }
}
