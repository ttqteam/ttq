package ttq.dsl.yaml

import org.yaml.snakeyaml.Yaml
import scala.collection.JavaConverters._
import ttq.dsl.Class

object YamlParser {
  def readOntologyClass(str: String) : Class = {
    val yaml = new Yaml()
    val m: java.util.Map[String, Any] = yaml.load(str)
    FactsTableParser.parse(m.asScala.toMap)
  }
}
