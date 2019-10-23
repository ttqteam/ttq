package ttq.dsl.yaml

import scala.collection.JavaConverters._
import ttq.dsl.{Aggregate, Class, Dimension}

object FactsTableParser {
  def parse(m: Map[String, Any]): Class = {

    val name: String = m.get("name") match {
      case Some(s:String) => s
      case _ => throw new Exception("String \"name\" must by specified")
    }

    val sql: String = m.get("sql") match {
      case Some(s:String) => s
      case _ => throw new Exception("String \"sql\" must by specified")
    }

    val dimensions: List[Dimension] = m.get("dimensions") match {
      case Some(x:java.util.ArrayList[_]) => x.asScala.toList.map(DimensionParser.parse)
      case _ => List()
    }

    val aggregates: List[Aggregate] = m.get("aggregates") match {
      case Some(x:java.util.ArrayList[_]) => x.asScala.toList.map(AggregateParser.parse)
      case _ => List()
    }

    val hide: List[String] = m.get("hide") match {
      case Some(jl: java.util.List[_]) => jl.asInstanceOf[java.util.List[String]].asScala.toList
      case Some(s: String) => s.split(',').map(_.trim).toList
      case Some(_) => throw new Exception("Property \"hide\" must contain a list of strings")
      case _ => List()
    }

    val ord: Option[String] = m.get("orderBy") match {
      case None => None
      case Some(s: String) => Some(s)
      case _ => throw new Exception("Property  \"orderBy\" must by a string")
    }

    val cls = new Class(name).fromSql(sql)
    dimensions.foreach(cls.addDimension)
    aggregates.foreach(cls.addAggregate)
    hide.foreach(cls.addHide)
    ord.foreach(cls.orderBy)

    cls
  }
}
