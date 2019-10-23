package ttq.tokenizer

import org.scalatest.FunSuite
import java.time.{LocalDate, LocalDateTime}

class DateParserTest extends FunSuite {

//  test("test full dates") {
//    val validDates = List(
//       "11-mar-2019"
//      ,"11 mar 2019"
//      ,"11 March 2019"
//      ,"11/March/2019"
//      ,"March 11 2019"
//      ,"March 11, 2019"
//      ,"2019 March 11"
//
//      ,"2019.3.11"
//      ,"2019-3-11"
//      ,"2019/3/11"
//      ,"2019/03/11"
//      ,"11/3/2019"
//    )
//
//    for (d <- validDates) {
//      val dd = DateParser.parse(d, LocalDateTime.of(2019,2,28, 0,0))
//      assert(dd.isDefined, s"Did not parse date '$d'")
//      assert(dd.get.equals(LocalDate.of(2019, 3, 11)), s"Date '$d' wrongly parsed as ${dd.get}")
//    }
//  }
//
//  test("test partial dates") {
//    val now = LocalDateTime.of(2019,2,28, 0,0)
//    assert(DateParser.parse("2017", now).get == LocalDate.of(2017, 1, 1))
//
//    assert(DateParser.parse("feb", now).get == LocalDate.of(2019, 2, 1)) // before current month - this year
//    assert(DateParser.parse("apr", now).get == LocalDate.of(2018, 4, 1)) // after curent month - past year
//
//    assert(DateParser.parse("2018 apr", now).get == LocalDate.of(2018, 4, 1))
//    assert(DateParser.parse("apr 2018", now).get == LocalDate.of(2018, 4, 1))
//  }
//
//  test("test relative dates") {
//    val now = LocalDateTime.of(2019,2,28, 0,0)
//    // "now" is Thursday
//    assert(DateParser.parse("sat", now).get == LocalDate.of(2019, 2, 23)) // Sat > Thu => past week
//    assert(DateParser.parse("mon", now).get == LocalDate.of(2019, 2, 25)) // Mon < Thu => this week
//    assert(DateParser.parse("this week", now).get == LocalDate.of(2019, 2, 25))
//    assert(DateParser.parse("this month", now).get == LocalDate.of(2019, 2, 1))
//    assert(DateParser.parse("this year", now).get == LocalDate.of(2019, 1, 1))
//    assert(DateParser.parse("today", now).get == LocalDate.of(2019, 2, 28))
//  }
}
