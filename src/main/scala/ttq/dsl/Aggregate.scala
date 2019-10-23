package ttq.dsl

import ttq.common.{DataType, NumberType}

import scala.collection.mutable.ArrayBuffer

class Aggregate(val name: String) extends Property {
  /*
  * Sql which was set by client
  * */
  private var sql: String = _

  private var units: Option[String] = None

  private val synonyms = new ArrayBuffer[String]()

  def fromSql(sql: String): Aggregate = {
    this.sql = sql
    this
  }

  def addSynonym(synonym: String): Aggregate = {
    this.synonyms += synonym
    this
  }

  def setUnits(units: String): Aggregate = {
    this.units = Some(units)
    this
  }

  override def getSql: String = sql

  override def dataType: DataType = NumberType

  override def getSynonyms: List[String] = synonyms.toList

  def getUnits: Option[String] = units

  def canEqual(other: Any): Boolean = other.isInstanceOf[Aggregate]

  override def equals(other: Any): Boolean = other match {
    case that: Aggregate =>
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
