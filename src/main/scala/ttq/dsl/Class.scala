package ttq.dsl

import scala.collection.mutable.ArrayBuffer

class Class(val name: String) {
  private var sql: String = _
  private var ord: Option[String] = None
  val aggregates = new ArrayBuffer[Aggregate]()
  val dimensions = new ArrayBuffer[Dimension]()
  val measures = new ArrayBuffer[Measure]()
  val hide = new ArrayBuffer[String]()

  def fromSql(sql: String) : Class = {
    this.sql = sql
    this
  }

  def fromTable(tblName: String) : Class = {
    this.sql = tblName
    this
  }

  def addDimension(dimension: Dimension) : Class = {
    this.dimensions += dimension
    this
  }

  def addMeasure(measure: Measure) : Class = {
    this.measures += measure
    this
  }

  def addAggregate(aggregate: Aggregate) : Class = {
    this.aggregates += aggregate
    this
  }

  def addHide(name: String): Class = {
    this.hide.append(name)
    this
  }

  def orderBy(orderBy: String): Class = {
    this.ord = Some(orderBy)
    this
  }

  def getSql: String = sql

  def getOrderBy: Option[String] = ord

  def prettyPrint():Unit = {
    println(s"################class $name################")
    println("------------------measures------------------")
    measures.foreach(measure => {
      measure.prettyPrint()
      println()
    })
    println("------------------dimensions------------------")
    dimensions.foreach(dimension => {
      dimension.prettyPrint()
      println()
    })
    println("------------------aggregates------------------")
    aggregates.foreach(aggregate => {
      aggregate.prettyPrint()
      println()
    })
    println(s"###############################################")
  }
}
