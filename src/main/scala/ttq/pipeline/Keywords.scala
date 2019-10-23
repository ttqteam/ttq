package ttq.pipeline

import scala.collection.mutable

case object Keywords {
  private val lst =  mutable.ArrayBuffer[String]()
  private def add(s: String): String = {
    lst.append(s)
    s
  }

  def list: List[String] = lst.toList

  val and: String = add("and")
  val or: String = add("or")
  val by: String = add("by")
  val where: String = add("where")
  val lastMonth: String = add("last month")
  val eq: String = add("=")
  val gt: String = add(">")
  val lt: String = add("<")
  val lparen: String = add("(")
  val rparen: String = add(")")
}

