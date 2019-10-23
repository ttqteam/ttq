package ttq.dsl.yaml

import ttq.dsl.Aggregate

import scala.collection.JavaConverters._

object AggregateParser {
  def parse(y: Any): Aggregate = {

    y match {
      case jm: java.util.Map[_,_] => {
        val m = jm.asInstanceOf[java.util.Map[String, Any]].asScala.toMap

        val name: String = m.get("name") match {
          case Some(s: String) => s
          case _ => throw new Exception("Property \"name\" must by specified for aggregate")
        }

        val sql: String = m.get("sql") match {
          case Some(s: String) => s
          case _ => throw new Exception("Property \"sql\" must by specified for aggregate")
        }

        val units: Option[String] = m.get("units") match {
          case Some(s) => s match {
            case x: String => Some(x)
            case _ => throw new Exception("Property \"units\" must by a string")
          }
          case None => None
        }

        val synonyms: List[String] = m.get("synonyms") match {
          case Some(jl: java.util.List[_]) =>
            jl.asInstanceOf[java.util.List[String]].asScala.toList
          case Some(s: String) => s.split(',').map(_.trim).toList
          case Some(_) => throw new Exception("Property \"synonyms\" must contain a list of strings")
          case _ => List()
        }

        val a = new Aggregate(name).fromSql(sql)
        units.foreach(a.setUnits)
        synonyms.foreach(a.addSynonym)

        a
      }
      case _ => throw new Exception("\"dimension\" definition must be a YAML structure (with fields like \"name: myName\" etc.)")
    }
  }
}
