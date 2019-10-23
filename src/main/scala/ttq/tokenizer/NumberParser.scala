package ttq.tokenizer

import java.text.{NumberFormat, ParsePosition}


object NumberParser {
  private val fmt = NumberFormat.getIntegerInstance(java.util.Locale.US)

  def parse(str: String): Option[Long] = {
    // todo - not hardcoded locale
    val parsePosition = new ParsePosition(0)
    val result = fmt.parse(str, parsePosition)
    if (parsePosition.getIndex == 0)
      return None;

    val numberSuffix = str.substring(parsePosition.getIndex).trim.toUpperCase
    val mult: Long = numberSuffix match {
      case "K" => 1000
      case "M" => 1000 * 1000
      case "B" => 1000 * 1000 * 1000
      case _ => 1
    }

    result match {
      // todo: check this long cast
      case i: java.lang.Long => return Some(i * mult)
      case _ => return None
    }
  }
}
