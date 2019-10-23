package ttq.tokenizer

import java.time.LocalDate

import ttq.common._
import ttq.ontology.FinalClass

sealed trait Token {
  def name: String
  override def toString: String = s"[$name]"
}

case class FactsToken(ref: FactsRef) extends Token {
  override def name: String = ref.name
}

//case class MeasureToken(ref: MeasureRef) extends Token {
//  override def name: String = ref.name
//}
//
case class DimensionToken(ref: DimensionRef) extends Token {
  override def name: String = ref.name
}

case class AggregateToken(ref: AggregateRef) extends Token {
  override def name: String = ref.name
}

// just some word, unparsed yet
case class WordToken(name: String) extends Token {
  override def toString: String = s"$name"
}

// word parsed to a known keyword
case class KeywordToken(name: String) extends Token {
  override def toString: String = name
}

case class StringEntityToken(dimension: DimensionRef, name: String) extends Token

case class NumberToken(value: Number) extends Token {
  override def name: String = value.toString
}

case class DateToken(value: LocalDate) extends Token {
  override def name: String = value.toString
}
