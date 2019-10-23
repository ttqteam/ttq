package ttq.dsl.yaml

import ttq.common.{BooleanType, DataType, DateType, NumberType, StringType}

import scala.collection.JavaConverters._
import ttq.dsl.Dimension

object DimensionParser {
  def parse(y: Any): Dimension = {

    y match {
      case jm: java.util.Map[_,_] => {
        val m = jm.asInstanceOf[java.util.Map[String, Any]].asScala.toMap

        val name: String = m.get("name") match {
          case Some(s: String) => s
          case _ => throw new Exception("Property \"name\" must by specified for dimension")
        }

        val sql: String = m.get("sql") match {
          case Some(s: String) => s
          case _ => throw new Exception("Property \"sql\" must by specified for dimension")
        }

        val dataType: Option[DataType] = m.get("type") match {
          case Some("number") => Some(NumberType)
          case Some("date")   => Some(DateType)
          case Some("string") => Some(StringType)
          case Some("bool") => Some(BooleanType)
          case Some(_) => throw new Exception("Property \"dataType\" must be one of {number, date, string}")
          case _ => None
        }

        val synonyms: List[String] = m.get("synonyms") match {
          case Some(jl: java.util.List[_]) =>
            jl.asInstanceOf[java.util.List[String]].asScala.toList
          case Some(s: String) => s.split(',').map(_.trim).toList
          case Some(_) => throw new Exception("Property \"synonyms\" must contain a list of strings")
          case _ => List()
        }

        val d = new Dimension(name).fromSql(sql)
        dataType.foreach(d.dataType)
        synonyms.foreach(d.addSynonym)

        d
      }
      case _ => throw new Exception("\"dimension\" definition must be a YAML structure (with fields like \"name: myName\" etc.)")
    }
  }
}
