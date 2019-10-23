package ttq.execution.readers

import java.sql.ResultSet
import java.time.LocalDate

import ttq.common.DateType
import ttq.execution.{ExecutionResult, ResultReader, SeriesInfo, resultSetIterator}

/*
table
x  y  z r1 r2
---------------
a  b  c  1  2
c  d  e  3  4
*/
case class TableResultReader(columns: Seq[SeriesInfo]) extends ResultReader {
  override def readResult(rs: ResultSet, modifiers: Seq[String]): ExecutionResult = {
    // list of functions to read all columns (convert data types)
    val columnReaders = columns.zipWithIndex.map(ci => ci._1.dataType match {
      case DateType => (r:ResultSet) => r.getDate(ci._2 + 1).toLocalDate
      case _ => (r:ResultSet) => r.getObject(ci._2 + 1)
    })

    val rows = resultSetIterator(rs).map(r => columnReaders.map(cr => cr(r))).toSeq

    ExecutionResult.table("", columns, rows)
  }
}
