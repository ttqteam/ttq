package ttq.common

import java.sql.{DriverManager, ResultSet}
import java.util.Properties

import com.typesafe.scalalogging.Logger
import ttq.config.DbConfig

class DbConnector(config: DbConfig) {
  private val logger = Logger[DbConnector]
  val conn = DriverManager.getConnection(
    config.url,
    config.parameters.foldLeft(new Properties())((props,x) => {props.setProperty(x._1, x._2); props})
  )

  def executeQuery(sql: String): ResultSet = {
    logger.info("Executing SQL: " + sql)
    conn.createStatement().executeQuery(sql)
  }
}
