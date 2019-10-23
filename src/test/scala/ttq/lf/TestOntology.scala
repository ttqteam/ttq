package ttq.lf

import ttq.common._
import ttq.ontology.{FinalClass, FinalOntology}

object TestOntology {
  val ORDERS_FACTS = FactsRef("orders_tbl", "orders", "orders")
  val ASSET_DIM = DimensionRef("product", "product", StringType, "orders")
  val CITY_DIM = DimensionRef("city", "city", StringType, "orders")
  val PRICE_DIM = DimensionRef("price", "price", NumberType, "orders")
  val ORDER_DATE_DIM = DimensionRef("order_date", "order_date", DateType, "orders")
  val BUY_SELL_AGG = AggregateRef("count(buy)/count(sell)", "buy_sell_ratio", "orders")
  val ORDERS_CLASS = FinalClass("orders", "orders_tbl", List(ORDERS_FACTS), List(BUY_SELL_AGG), List(), List(ASSET_DIM, PRICE_DIM), Set())
  val ontology = FinalOntology(List(ORDERS_CLASS))
}
