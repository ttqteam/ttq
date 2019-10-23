package ttq.tokenizer

import java.time.temporal.TemporalAdjusters
import java.time.{DayOfWeek, Instant, LocalDate, LocalDateTime}

object DateParser {
  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  // todo: this probably compiles all regexpes every time match is done, so performance hit (move regexp outside)
  def parse(str: String, now: LocalDateTime): Option[LocalDate] = {
    try {
      str.toLowerCase match {
//        case r"(\d{1,2})$d[^\w]+([a-zA-Z]{3,})$mon[^\w]+(\d\d\d\d)$yyyy" => makeDateLongMonth(yyyy, mon, d) // 11 apr 2019
//        case r"([a-zA-Z]{3,})$mon[^\w]+(\d{1,2})$d[^\w]+(\d\d\d\d)$yyyy" => makeDateLongMonth(yyyy, mon, d) // apr 11 2019
//        case r"(\d\d\d\d)$yyyy[^\w]+([a-zA-Z]{3,})$mon[^\w]+(\d{1,2})$d" => makeDateLongMonth(yyyy, mon, d) // 2019 apr 11

        // todo - american standard (m/d/yyyy) not supported
//        case r"(\d{1,2})$d[/.-](\d{1,2})$m[/.-](\d\d\d\d)$yyyy" => makeDateShortMonth(yyyy, m, d) // d-m-2019
        case r"(\d\d\d\d)$yyyy[/.-](\d{1,2})$m[/.-](\d{1,2})$d" => makeDateShortMonth(yyyy, m, d) // 2019-m-d

//        case r"(\d\d\d\d)$yyyy" => makeDateFromYear(yyyy, now) // 2019
//
//        case r"([a-zA-Z]{3,})$monOrWeekday" => makeDateFromMonthStr(monOrWeekday, now)  // april
//          .orElse(makeDateFromWeekDay(monOrWeekday, now)) //  sunday
//          .orElse(makeDateFromToday(now)) // today
//
//        case r"(\d\d\d\d)$yyyy[^\w]+([a-zA-Z]{3,})$mon" => makeDateFromMonthStrYear(yyyy, mon, now) // 2019 april
//        case r"([a-zA-Z]{3,})$mon[^\w]+(\d\d\d\d)$yyyy" => makeDateFromMonthStrYear(yyyy, mon, now) // april 2019
//
//        case r"this[^\w]week" => makeDateFromThisWeek(now) // this week
//        case r"this[^\w]month" => makeDateFromThisMonth(now) // this month
//        case r"this[^\w]year" => makeDateFromThisYear(now) // this year

        case _ => None
      }
    } catch {
      case _: RuntimeException => None
    }
  }

  def makeDateFromThisWeek(now: LocalDateTime): Option[LocalDate] = {
    Some(now.`with`(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate)
  }

  def makeDateFromToday(now: LocalDateTime): Option[LocalDate] = {
    Some(now.toLocalDate)
  }

  def makeDateFromThisMonth(now: LocalDateTime): Option[LocalDate] = {
    Some(LocalDate.of(now.getYear, now.getMonth, 1))
  }

  def makeDateFromThisYear(now: LocalDateTime): Option[LocalDate] = {
    Some(LocalDate.of(now.getYear, 1, 1))
  }

  def makeDateFromWeekDay(weekday: String, now: LocalDateTime): Option[LocalDate] = {
    val dow = parseDayOfWeek(weekday)
    if (dow == null) return None
    Some(now.`with`(TemporalAdjusters.previousOrSame(dow)).toLocalDate)
  }

  def makeDateFromMonthStrYear(year: String, month: String, now: LocalDateTime) = {
    val y = year.toInt
    val m = parseMonth(month)
    if (y >= 1900 && y <= now.getYear && m > 0)
      makeDate(y, m, 1)
    else
      None
  }

  def makeDateFromYear(year: String, now: LocalDateTime) = {
    val y = year.toInt
    if (y >= 1900 && y <= now.getYear)
      makeDate(y,1,1)
    else
      None
  }

  def makeDateFromMonthStr(month: String, now: LocalDateTime) = {
    val m = parseMonth(month)
    if (m > 0 && m <= now.getMonth.getValue)
      makeDate(now.getYear, m, 1)
    else if (m > 0 && m > now.getMonth.getValue)
      makeDate(now.getYear-1, m, 1)
    else
      None
  }

  private def makeDateShortMonth(year:String, month:String, day:String): Option[LocalDate] = {
    val y = year.toInt
    val d = day.toInt
    val m = month.toInt

    makeDate(y,m,d)
  }

  private def makeDateLongMonth(year:String, month:String, day:String): Option[LocalDate] = {
    val y = year.toInt
    val d = day.toInt
    val m = parseMonth(month)
    if (m < 1) return None

    makeDate(y, m, d)
  }

  private def parseMonth(month: String) = {
    month match {
      case "jan" | "january" => 1
      case "feb" | "february" => 2
      case "mar" | "march" => 3
      case "apr" | "april" => 4
      case "may" | "may" => 5
      case "jun" | "june" => 6
      case "jul" | "july" => 7
      case "aug" | "august" => 8
      case "sep" | "september" => 9
      case "oct" | "october" => 10
      case "nov" | "november" => 11
      case "dec" | "december" => 12
      case _ => -1
    }
  }

  private def parseDayOfWeek(dayOfWeek: String) = {
    dayOfWeek match {
      case "mon" | "monday" => DayOfWeek.MONDAY // it used to be a weekend day in February :(
      case "tue" | "tuesday" => DayOfWeek.TUESDAY
      case "wed" | "wednesday" => DayOfWeek.WEDNESDAY
      case "thu" | "thursday" => DayOfWeek.THURSDAY
      case "fri" | "friday" => DayOfWeek.FRIDAY
      case "sat" | "saturday" => DayOfWeek.SATURDAY
      case "sun" | "sunday" => DayOfWeek.SUNDAY
      case _ => null // don't want to box into option
    }
  }
  private def makeDate(y:Int, m:Int, d:Int) = {
    // todo: it will throw on invalid dates (march 35)- we probably want to fix them
    Some(LocalDate.of(y, m, d))
  }

}
