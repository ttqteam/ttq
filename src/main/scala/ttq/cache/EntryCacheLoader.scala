package ttq.cache

import ttq.common.{DbConnector, DimensionRef, StringType}
import ttq.ontology.FinalOntology


class EntryCacheLoader(ontology: FinalOntology, dbConnector: DbConnector) {
  private val limit = 0

  def fillCache(cache: EntryCacheImpl) = {
    ontology.classes
      .flatMap(_.dimensions)
      .filter(_.dataType == StringType)
      .foreach(d => fillDimension(cache, d))
  }

  private def fillDimension(cache: EntryCacheImpl, dimension: DimensionRef) = {
    val finalClass = ontology.getClass(dimension.className)
    // NOTE that we ORDER BY count(*) ASC
    // because then it goes to List which is iterated backwards (LIFO)
    val sql =
      s"""SELECT ${dimension.sql}, count(*) FROM ${finalClass.sql}
         |WHERE (${dimension.sql}) <> '' AND (${dimension.sql}) IS NOT NULL
         |GROUP BY (${dimension.sql})
         |ORDER BY count(*) ASC
         |${if (limit > 0) " LIMIT " + limit.toString else ""}""".stripMargin
    try {
      val res = dbConnector.executeQuery(sql)
      var rows: List[String] = List[String]()
      while (res.next()) rows = res.getString(1) :: rows
      cache.setEntitiesSortedByUsageDesc(dimension, rows)
    } catch {
      case e: Throwable => throw new Exception(s"""Exception executing query "$sql"""", e)
    }
  }
}
