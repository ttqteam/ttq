package ttq.pipeline

import com.typesafe.scalalogging.Logger
import org.springframework.util.StopWatch
import ttq.common._
import ttq.lf._
import ttq.ontology.FinalOntology
import ttq.parser.{ParseResult, PdaParser}
import ttq.tokenizer.{KnowledgeBase, StringEntityToken, Token, Tokenizer}

import scala.collection.mutable
import ttq.cache.EntryCache
import ttq.execution.{ExecutionResult, PlanToSqlConverter, QueryToPlanConverter, QueryToResultReaderConverter}

class Executor(ontology: FinalOntology, entryCache: EntryCache, dbConnector: DbConnector) {
  private val logger = Logger[Executor]
  private val tokenizer = new Tokenizer(KnowledgeBase.fromOntology(ontology, entryCache))
  private val parser = new PdaParser[Token, Option[Token]](TheGrammar)
  private val queryToPlanConverter = new QueryToPlanConverter(ontology)
  private val queryToResultReaderConverter = new QueryToResultReaderConverter(ontology)

  /***
    * @param modifiers IDs of the query modifiers (whatever IDs are returned in result)
    */
  def execute(userId: UserId, tokens: List[Token], modifiers: Seq[String]): ExecutionResult = {
    val stopwatch = new StopWatch(s"Executor for $tokens")

    stopwatch.start("Tokenize query")
    // todo: taking head - check if exist
    val tokenization = tokenizer.parseUserInput(tokens, -1).head.value
    logger.info(s"Tokenized $tokens to " + tokenization)

    // update usage cache
    tokenization
      .collect {case t : StringEntityToken => t}
      .foreach(t => entryCache.recordEntityHit(userId, t.dimension, t.name))

    stopwatch.stop()
    stopwatch.start("Parse tokenization")

    val parseResult:List[Probable[ParseResult]] = parser.parse(tokenization, 0)
    logger.info("Parsed " + tokenization + s" into:\n  " + parseResult.mkString("\n  "))
    // todo: validate?
    // todo - maybe handle parse results more robustly
    assert(parseResult.length == 1)
    val query = parseResult.head.value.expression.asInstanceOf[Query]
    logger.info("Parsed " + tokens + " into query " + query)

    stopwatch.stop()
    stopwatch.start("Query DB")

    val plan = queryToPlanConverter.queryToPlan(query)
    val sqlQuery = PlanToSqlConverter.planToSql(plan, modifiers)
    val resultReader = queryToResultReaderConverter.queryToResultReader(query)
    logger.info(s"Executing SQL query: " + sqlQuery.sql)
    val rs = dbConnector.executeQuery(sqlQuery.sql)
    val result = resultReader.readResult(rs, modifiers)

    stopwatch.stop()
    stopwatch.start("Post-process")

    val title = QueryToTokensConverter.convert(query).map(t => t.name).mkString(" ")
    val units = query.getResultUnits

    stopwatch.stop()

    logger.info(stopwatch.prettyPrint())
    result
  }

  private def getQueryRes(sqlQuery: SqlQuery): (List[Double], List[String]) = {
    val rs = dbConnector.executeQuery(sqlQuery.sql)

    val measureValues = mutable.ArrayBuffer[Double]()
    val dimensionLabels = mutable.ArrayBuffer[String]()
    while (rs.next()) {
      measureValues.append(roundAt(2, rs.getDouble(1))) // rounding to 2 digits!
      sqlQuery match {
        case _: ttq.lf.SqlQueryScalar => // for scalar results we don't have "dimension" labels
        case _: ttq.lf.SqlQueryTable => dimensionLabels.append(rs.getString(2))

      }
    }

    (measureValues.toList, dimensionLabels.toList)
  }


  def roundAt(precision: Int, value: Double): Double = {
    val s = math pow (10, precision)
    (math round value * s) / s
  }
}
