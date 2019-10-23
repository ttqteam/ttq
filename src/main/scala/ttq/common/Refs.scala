package ttq.common

import java.time.LocalDate

case class FactsRef(sql: String, name: String, className: String, orderBy: Option[String] = None, synonyms: List[String] = List()) {
  override def toString: String = name
}

sealed trait FinalProperty {
  def sql: String
}

case class AggregateRef(sql: String, name: String, className: String, units: Option[String] = None, synonyms: List[String] = List()) extends FinalProperty {
  override def toString: String = name
}

case class DimensionRef(sql: String, name: String, dataType: DataType, className: String, synonyms: List[String] = List()) extends FinalProperty {
  override def toString: String = name
}

case class MeasureRef(sql: String, name: String, dataType: DataType, className: String, synonyms: List[String] = List()) extends FinalProperty {
  override def toString: String = name
}

sealed trait EntityRef

case class StringEntityRef(dimension: DimensionRef, value: String) extends EntityRef {
  override def toString: String = value
}

case class NumberEntityRef(value: Number) extends EntityRef {
  override def toString: String = value.toString
}

case class DateEntityRef(value: LocalDate) extends EntityRef {
  override def toString: String = value.toString
}
