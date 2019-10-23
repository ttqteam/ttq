package ttq.pipeline

import scala.util.{Failure, Success, Try}
import com.typesafe.scalalogging.Logger
import org.springframework.util.StopWatch
import ttq.cache.EntryCache
import ttq.common._
import ttq.parser.PdaParser
import ttq.lf._
import ttq.ontology.FinalOntology
import ttq.tokenizer._

class Pipeline(ontology: FinalOntology, entryCache: EntryCache) {
  private val logger = Logger[Pipeline]
  private val maxParserDistance = 3
  private val tokenizerTopPercent = 0.0
  private val parserTopPercent = 0.50
  private val tokenizerWeightPower = 1.0
  private val tokenizerTopN: Int = 5
  private val parserTopN: Int = 10

  val parser = new PdaParser[Token, Option[Token]](TheGrammar)
  val tokenizer = new Tokenizer(KnowledgeBase.fromOntology(ontology, entryCache))

  def parse(userId: UserId, tokens: List[Token], curToken: Int = -1, cancellationToken: CancellationToken): List[PipelineResult] = {
    val stopwatch = new StopWatch(s"Pipeline for $tokens")
    stopwatch.start("Tokenize query")
    val allTokenizations = tokenizer.parseUserInput(tokens, curToken)
    val tokenizations = getTop(tokenizerTopPercent, allTokenizations).take(tokenizerTopN)
    logger.info(s"Tokenized $tokens to ${allTokenizations.size} variants. Top results:\n  " + tokenizations.mkString("\n  "))
    stopwatch.stop()

    stopwatch.start("Parse tokenizations")
    val distinctParsed = tokenizations.par.flatMap(t => {
      val res = parser
        .parse(t.value, maxParserDistance, cancellationToken)
        .map(q => Probable(q.value, q.probability * math.pow(t.probability, tokenizerWeightPower)))
        .groupBy(_.value.expression)
        .map(r => Probable(r._1, r._2.maxBy(_.probability).probability))
        .toList
        .sortBy(p => -p.probability)
      logger.info("Parsed " + t + s" into:\n  " + res.mkString("\n  "))
      res
    })
    stopwatch.stop()

    stopwatch.start("Queries validation and expansion")
    val queryValidator = new QueryValidator(ontology, entryCache, userId)
    val validated = distinctParsed.flatMap(q =>
        queryValidator
          .validateAndExpandQuery(q.value.asInstanceOf[Query])
          .map(v => Probable(v, q.probability))
      )
      .groupBy(_.value)
      .map(r => Probable(r._1, r._2.maxBy(_.probability).probability))
      .toList
      .sortBy(p => -p.probability)
      .take(parserTopN)
    val topValidated = getTop(parserTopPercent, validated)
    logger.info(s"Validated & expanded (top $parserTopN) " + validated.map("\n  " + _))
    stopwatch.stop()

    stopwatch.start("Queries to tokens conversion")
    val results = topValidated
      .map(v => Try(PipelineResult(QueryToTokensConverter.convert(v.value), v.value)))
      .flatMap {
        case Success(s) => Some(s)
        case Failure(e) => logger.error("Error converting query to tokens", e); None
      } // replace with tap() in scala 2.13
  //      .sortBy(ts => ts.tokens.map(_.name).mkString(" "))
    stopwatch.stop()

    logger.info(stopwatch.prettyPrint())
    results
  }

  private def getTop[T](fraction: Double, xs: List[Probable[T]]) = xs match {
    case Nil => Nil
    case _ =>
      val maxP = xs.maxBy(r => r.probability).probability
      xs.filter(r => r.probability >= maxP * fraction)
  }
}
