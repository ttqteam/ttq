package ttq.dsl

import ttq.common.{DataType, StringType}

import scala.collection.mutable.ArrayBuffer

class Dimension(val name: String) extends Property {
  /*
  * Sql which was set by client (or just column in case of 1-1 mapping)
  * */
  private var sql: String = _
  private var dt: DataType = StringType

  /*
  * Natural language description of relation between class and dimension.
  * E.g. for class "email" and dimension "receiver" rel name could be "to"
  * */
  // TODO - start using or remove before first release
  private var relName: String = _

  private val synonyms = new ArrayBuffer[String]()

  def fromSql(sql: String): Dimension = {
    this.sql = sql
    this
  }

  def dataType(dataType: DataType): Dimension = {
    this.dt = dataType
    this
  }

  def addSynonym(synonym: String): Dimension = {
    this.synonyms += synonym
    this
  }

  def addRelName(relName: String): Dimension = {
    this.relName = relName
    this
  }

  override def getSql: String = sql

  override def dataType: DataType = dt

  override def toString: String = name

  override def getSynonyms: List[String] = synonyms.toList

  def canEqual(other: Any): Boolean = other.isInstanceOf[Dimension]

  override def equals(other: Any): Boolean = other match {
    case that: Dimension =>
      (that canEqual this) &&
      // todo
        toString == that.toString
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(name)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  def prettyPrint(): Unit = {
    println(s"name: $name")
    println(s"sql: $sql")
  }
}