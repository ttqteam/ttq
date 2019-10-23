package ttq.dsl

import ttq.common.{DataType, NumberType}

import scala.collection.mutable.ArrayBuffer

class Measure(val name: String) extends Property {
  /*
  * Sql which was set by client (or just column in case of 1-1 mapping)
  * */
  private var sql: String = _
  private var dt: DataType = NumberType

  private val synonyms = new ArrayBuffer[String]()

  def fromSql(sql: String): Measure = {
    this.sql = sql
    this
  }

  def dataType(dataType: DataType): Measure = {
    this.dt = dataType
    this
  }

  def addSynonym(synonym: String): Measure = {
    this.synonyms += synonym
    this
  }

  override def getSql: String = sql

  override def dataType: DataType = dt

  override def getSynonyms: List[String] = synonyms.toList

  def canEqual(other: Any): Boolean = other.isInstanceOf[Measure]

  override def equals(other: Any): Boolean = other match {
    case that: Measure =>
      (that canEqual this) &&
      // todo
        toString == that.toString
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(name)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }


  override def toString: String = name

  def prettyPrint(): Unit = {
    println(s"name: $name")
    println(s"sql: $sql")
  }
}
