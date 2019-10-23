package ttq.execution.readers

import java.sql.ResultSet
import ttq.execution.{ExecutionResult, ResultReader, roundAt}

case class ScalarReader(units: Option[String]) extends ResultReader {
  override def readResult(rs: ResultSet, modifiers: Seq[String]): ExecutionResult = {
    rs.next()
    val value = roundAt(2, rs.getDouble(1))
    ExecutionResult.scalar("", value, units)
  }
}
