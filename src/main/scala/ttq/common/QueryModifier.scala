package ttq.common

/***
  * Represent query modifier like "by week"
  * @param id Whatever (to be used to reference modifier in UI-server communication)
  * @param label Text shown to user.
  */
sealed trait QueryModifier {
  def id: String
  def label: String
}

sealed trait DateQueryModifier extends QueryModifier

case object ByDayQueryModifier extends DateQueryModifier {
  override def id: String = "byDay"
  override def label: String = "by day"
}

case object ByWeekQueryModifier extends DateQueryModifier {
  override def id: String = "byWeek"
  override def label: String = "by week"
}

case object ByMonthQueryModifier extends DateQueryModifier {
  override def id: String = "byMonth"
  override def label: String = "by month"
}

case object ByYearQueryModifier extends DateQueryModifier {
  override def id: String = "byYear"
  override def label: String = "by year"
}

case class QueryModifierWithStatus(queryModifier: QueryModifier, isActive: Boolean)

object QueryModifierUtils {
  private val DATE_MODIFIERS = ByDayQueryModifier::ByWeekQueryModifier::ByMonthQueryModifier::ByYearQueryModifier::Nil

  def getDateModifiers(activeNowOpt: Option[QueryModifier]): Seq[QueryModifierWithStatus] = {
    val active:QueryModifier = activeNowOpt.getOrElse(ByDayQueryModifier)

    DATE_MODIFIERS
      .map(modifier => (modifier, modifier == active))
      .map(pair => QueryModifierWithStatus(pair._1, pair._2))
  }

  def mapDateModifiers(modifiers: Seq[String]): Seq[DateQueryModifier] = {
    modifiers.flatMap(str => DATE_MODIFIERS.find(modifier => modifier.id.equals(str)))
  }
}
