package ttq.execution

import ttq.common.{DataType, NumberType, QueryModifierWithStatus}

case class SeriesInfo(title: String, dataType: DataType, units: Option[String])

case class ExecutionResult(
  title: String,
  chartType: ChartType,
  series: Seq[SeriesInfo],
  data: Seq[Seq[Any]],
  modifiers: Seq[QueryModifierWithStatus]
)

object ExecutionResult {
  def scalar(title: String, value: Double, units: Option[String]) = {
    ExecutionResult(title, ScalarChart, Seq(SeriesInfo("", NumberType, units)), Seq(Seq(value)), Seq())
  }

  def chart(
     title: String,
     seriesInfo: Seq[SeriesInfo],
     seriesData: Seq[Seq[Any]],
     modifiers: Seq[QueryModifierWithStatus]
  ) = ExecutionResult(title, VerticalBarChart, seriesInfo, seriesData, modifiers)

  def table(title: String, columnsInfo: Seq[SeriesInfo], rowsData: Seq[Seq[Any]]) = {
    ExecutionResult(title, TableChart, columnsInfo, rowsData, Seq())
  }
}

sealed trait ChartType
case object ScalarChart extends ChartType
case object HorizontalBarChart extends ChartType
case object VerticalBarChart extends ChartType
case object TableChart extends ChartType
