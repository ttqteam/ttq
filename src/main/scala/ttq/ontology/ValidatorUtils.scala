package ttq.ontology

object ValidatorUtils {
  def getInnerPropertyNames(sql: String): List[String] = {
    """(?<=(^|[^\$_a-zA-Z0-9])\$)([_a-zA-Z][_a-zA-Z0-9]*)""".r.findAllIn(sql).toList
  }
}
