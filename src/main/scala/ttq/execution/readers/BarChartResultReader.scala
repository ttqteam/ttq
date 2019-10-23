package ttq.execution.readers

import java.sql.ResultSet
import java.time.LocalDate
import java.time.temporal.{ChronoUnit, TemporalUnit}

import ttq.common.QueryModifierUtils.getDateModifiers
import ttq.common._
import ttq.execution._

import scala.collection.mutable.ArrayBuffer

/*
x r1 r2
--------
a  1  3
b  2  4

Each "r" column is a data series.
*/
case class BarChartResultReader(labelDataType: DataType, series: Seq[SeriesInfo]) extends ResultReader {
  private val resultFormattingByType: Map[DataType, (Seq[Seq[Any]], Option[QueryModifier]) => Seq[Seq[Any]]] = Map(
    (StringType, formatStringEntry),
    (DateType, formatDate)
  )

  def readResult(rs: ResultSet, modifiers: Seq[String]): ExecutionResult = {
    val data = ArrayBuffer[ArrayBuffer[Any]]()
    val optionDateModifier = QueryModifierUtils.mapDateModifiers(modifiers).headOption
    for (r <- resultSetIterator(rs)) {
      val label = rs.getString(1)

      if (labelDataType == DateType && data.nonEmpty) {
        val lastDate = LocalDate.parse(data.last(0).asInstanceOf[String])
        val thisRowDate = LocalDate.parse(label)

        data.appendAll(fillEmptyDateResults(lastDate, thisRowDate, optionDateModifier))
      }

      val r = ArrayBuffer[Any]()
      r.append(label)
      for (i <- series.indices) {
        r.append(roundAt(2, rs.getDouble(i + 2))) // +1 because ResultSet is 1-based, another +1 because first column was label
      }
      data.append(r)
    }

    val modifiersWithStatus = if (labelDataType == DateType) getDateModifiers(optionDateModifier) else Seq()

    val formattedData = if (resultFormattingByType.contains(labelDataType))
      resultFormattingByType(labelDataType).apply(data, optionDateModifier)
    else
      data

    ExecutionResult.chart("", series, formattedData, modifiersWithStatus)
  }

  private def fillEmptyDateResults(
      lastDate: LocalDate,
      currentDate: LocalDate,
      dateModifier: Option[DateQueryModifier]
  ): ArrayBuffer[ArrayBuffer[Any]] = {
    dateModifier match {
      case None => fillEmptyDateResults(lastDate, currentDate, ChronoUnit.DAYS)
      case Some(ByDayQueryModifier) => fillEmptyDateResults(lastDate, currentDate, ChronoUnit.DAYS)
      case Some(ByWeekQueryModifier) => fillEmptyDateResults(lastDate, currentDate, ChronoUnit.WEEKS)
      case Some(ByMonthQueryModifier) => fillEmptyDateResults(lastDate, currentDate, ChronoUnit.MONTHS)
      case Some(ByYearQueryModifier) => fillEmptyDateResults(lastDate, currentDate, ChronoUnit.YEARS)
    }
  }

  private def fillEmptyDateResults(
    lastDate: LocalDate,
    currentDate: LocalDate,
    temporalUnit: TemporalUnit
  ): ArrayBuffer[ArrayBuffer[Any]] = {
    val res = ArrayBuffer[ArrayBuffer[Any]]()
    var currentLastDate = lastDate

    while (currentLastDate.plus(1, temporalUnit).isBefore(currentDate)) {
      currentLastDate = currentLastDate.plus(1, temporalUnit)
      val r = ArrayBuffer.fill[Any](series.size + 1)(0.0)
      r(0) = currentLastDate
      res.append(r)
    }

    res
  }

  private def formatDate(data: Seq[Seq[Any]], modifier: Option[QueryModifier]): Seq[Seq[Any]] = {
    modifier match {
      case None => data.takeRight(60) //2 months
      case Some(ByDayQueryModifier) => data.takeRight(60) //2 months
      case Some(ByWeekQueryModifier) => data.takeRight(52) //1 year
      case Some(ByMonthQueryModifier) => data.takeRight(60) //5 years
      case Some(ByYearQueryModifier) => data
    }
  }

  private def formatStringEntry(data: Seq[Seq[Any]], modifier: Option[QueryModifier]): Seq[Seq[Any]] = {
    data.sortWith((x, y) => x.tail.head.toString.toDouble > y.tail.head.toString.toDouble).take(20)
  }
}
