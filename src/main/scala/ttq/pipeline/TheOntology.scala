package ttq.pipeline

import ttq.common.{DateType, NumberType}
import ttq.dsl._

object TheOntology {
  /*
  * Should be defined by Max
  * */
  def createOntology: Ontology = new Ontology()
    .addClass(
      new Class("orders")
        .fromSql("orders_tbl")
        .addDimension(new Dimension("asset").fromSql("asset_column"))
        .addDimension(new Dimension("client").fromSql("client_column").addSynonym("counterparty"))
        .addAggregate(new Aggregate("buy/sell rate")
          .setUnits("%")
          .fromSql("1.0*sum(case when buy_sell_status='Buy' then 1 else 0 end)/sum(case when buy_sell_status in ('Buy','Sell') then 1 else 0 end)*100"))
        .addAggregate(new Aggregate("total orders").fromSql("count (*)").addSynonym("orders"))
    )
}