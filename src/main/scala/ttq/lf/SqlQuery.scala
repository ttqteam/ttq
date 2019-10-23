package ttq.lf

sealed trait SqlQuery { def sql: String }
case class SqlQueryScalar(sql: String) extends SqlQuery
case class SqlQueryTable(sql: String) extends SqlQuery



