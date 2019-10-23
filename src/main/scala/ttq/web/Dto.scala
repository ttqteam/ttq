package ttq.web

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{CustomSerializer, TypeHints}
import ttq.common.{BooleanType, DataType, DateType, NumberType, StringType}
import ttq.ontology.FinalOntology
import ttq.tokenizer._

case class HintRequestDto(editedToken: Int, tokens: List[DtoToken], sessionId: String)

case class DtoHint(tokens:List[DtoToken])

case class DtoQuery(tokens:List[DtoToken], modifiers: Seq[String], sessionId: String)

case class DtoSeriesInfo(title: String, dataType: String, units: Option[String])

case class DtoQueryResult(title: String, chartType: String, series: Seq[DtoSeriesInfo], values: Seq[Seq[Any]], modifiers: Seq[DtoQueryModifier])

case class DtoQueryModifier(id: String, label: String, active: Boolean)

sealed trait DtoToken {def text:String}
case class DtoWordToken(text:String) extends DtoToken
case class DtoKeywordToken(text:String) extends DtoToken
case class DtoFactsToken(text:String, name:String, table:String) extends DtoToken
case class DtoDimensionToken(text:String, name:String, table:String) extends DtoToken
//case class DtoMeasureToken(text:String, name:String, table:String) extends DtoToken
case class DtoAggregateToken(text:String, name:String, table:String) extends DtoToken
case class DtoStringToken(text:String, dimension: String, table: String) extends DtoToken
case class DtoNumberToken(text:String, value: Number) extends DtoToken
case class DtoDateToken(text:String, date:String) extends DtoToken
//case object DtoEQToken extends DtoToken
//case object DtoGTToken extends DtoToken
//case object DtoLTToken extends DtoToken

class TokenTypeHints() extends TypeHints {
  val map = Map[Class[_], String](
    classOf[DtoWordToken] -> "word",
    classOf[DtoKeywordToken] -> "keyword",
    classOf[DtoFactsToken] -> "facts",
    classOf[DtoDimensionToken] -> "dimension",
//    classOf[DtoMeasureToken] -> "measure",
    classOf[DtoAggregateToken] -> "aggregate",
    classOf[DtoStringToken] -> "string", // todo - rename to 'entity' ?
    classOf[DtoNumberToken] -> "number",
    classOf[DtoDateToken] -> "date",
//    DtoEQToken.getClass -> "eq",
//    DtoGTToken.getClass -> "gt",
//    DtoLTToken.getClass -> "lt",
  )
  val hints = map.keys.toList
  def hintFor(clazz: Class[_]) = map(clazz)
  def classFor(hint: String) = map.find(_._2 == hint).map(_._1)
}

class DtoConverter(ontology: FinalOntology) {
  private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
  private val numberFormatter = NumberFormat.getIntegerInstance(java.util.Locale.US)

  def fromDto(dtoToken: DtoToken): Token = dtoToken match {
    // todo: validate passed values?
    case DtoWordToken(text) => WordToken(text)
    case DtoKeywordToken(text) => KeywordToken(text)
    case DtoFactsToken(_, name, table) => FactsToken(ontology.getFacts(table, name).get) // TODO - get
    case DtoDimensionToken(_, name, table) => DimensionToken(ontology.getDimension(table, name).get) // TODO - get
    //    case DtoMeasureToken(_, name, table) => MeasureToken(ontology.getMeasure(table, name).get) // TODO - get
    case DtoAggregateToken(_, name, table) => AggregateToken(ontology.getAggregate(table, name).get) // TODO - get
    case DtoStringToken(str, dimension, table) => StringEntityToken(ontology.getDimension(table, dimension).get, str) // TODO - get
    case DtoNumberToken(_, value) => NumberToken(value)
    case DtoDateToken(_, date) => DateToken(LocalDate.parse(date))
  }

  def toDto(token: Token): DtoToken = token match {
    case WordToken(text) => DtoWordToken(text)
    case KeywordToken(text) => DtoKeywordToken(text)
    case FactsToken(ref) => DtoFactsToken(ref.name, ref.name, ref.className)
    case DimensionToken(ref) => DtoDimensionToken(ref.name, ref.name, ref.className)
    case AggregateToken(ref) => DtoAggregateToken(ref.name, ref.name, ref.className)
    case NumberToken(num) => DtoNumberToken(numberFormatter.format(num), num)
    case DateToken(date) => DtoDateToken(date.format(dateFormatter), date.toString)
    case StringEntityToken(dimension, str) => DtoStringToken(str, dimension.name, dimension.className)
  }

  def toDto(dataType: DataType): String = dataType match {
    case NumberType => "number"
    case DateType => "date"
    case StringType => "string"
    case BooleanType => "bool"
  }
}

case object LocalDateSerializer extends CustomSerializer[LocalDate](format => (
  {
    case JString(s) => LocalDate.parse(s)
    case JNull => null
  },
  {
    case d: LocalDate => JString(d.format(DateFormatter.DD_MON_YYYY))
  }
))
case object DateFormatter {
  def DD_MON_YYYY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
}



