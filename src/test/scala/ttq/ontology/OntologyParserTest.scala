package ttq.ontology

import org.scalatest.FunSuite
import ttq.common._
import ttq.dsl._

class OntologyParserTest extends FunSuite{
  test("derive final dimension") {
      val test = new Dimension("test").fromSql("$test1 - $test2")
      val test1 = new Dimension("test1").fromSql("$test2 + $test3")
      val test2 = new Dimension("test2").fromSql("x")
      val test3 = new Dimension("test3").fromSql("y * 100")
      val finalDimSqlSet = OntologyParser.deriveFinalProperties(List(test, test1, test2, test3), new Class("123"))
        .map(dim => dim.sql).toSet
      assert(Set("y * 100", "x", "(x) + (y * 100)", "((x) + (y * 100)) - (x)") == finalDimSqlSet)
  }

  test("generate final ontology") {
    val ontology = new Ontology()
      .addClass(
        new Class("orders")
          .fromTable("orders_tbl")
          .addDimension(new Dimension("asset").fromSql("asset"))
          .addDimension(new Dimension("order_date").fromSql("order_date"))
          .addMeasure(new Measure("buy").fromSql("case buy_sell_status when 'BUY' then 1 else 0 end"))
          .addMeasure(new Measure("sell").fromSql("case buy_sell_status when 'SELL' then 1 else 0 end"))
          .addAggregate(new Aggregate("buy_sell_ratio").fromSql("sum($buy)/sum($sell)"))
      )

    val finalOntology = OntologyParser.getFinalOntology(ontology)

    assert(1 == finalOntology.classes.size)
    assert("orders" == finalOntology.classes.head.name)
    val dimensions = finalOntology.classes.head.dimensions.sortBy(x => x.name)
    assert(dimensions == List(
        DimensionRef("asset", "asset", StringType, "orders"),
        DimensionRef("order_date", "order_date", StringType, "orders")
      )
    )

    val measures = finalOntology.classes.head.measures.sortBy(x => x.name)
    assert(
      List(
        MeasureRef("case buy_sell_status when 'BUY' then 1 else 0 end", "buy", NumberType, "orders"),
        MeasureRef("case buy_sell_status when 'SELL' then 1 else 0 end", "sell", NumberType, "orders")
      ) == measures
    )

    val aggregate = finalOntology.classes.head.aggregates.head

    assert(aggregate == AggregateRef(
      "sum((case buy_sell_status when 'BUY' then 1 else 0 end))/sum((case buy_sell_status when 'SELL' then 1 else 0 end))",
      "buy_sell_ratio",
      "orders"
    ))
  }
}
