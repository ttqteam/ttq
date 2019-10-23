package ttq.web

import com.typesafe.scalalogging.Logger
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json._
import ttq.cache.EntryCache
import ttq.common.{CancellationSource, DbConnector}
import ttq.execution.{HorizontalBarChart, ScalarChart, TableChart, VerticalBarChart}
import ttq.ontology.FinalOntology
import ttq.pipeline._

import scala.collection.mutable
import scala.concurrent.CancellationException

class HintServlet(
  ontology: FinalOntology,
  entryCache: EntryCache,
  dbConnector: DbConnector) extends ScalatraServlet with JacksonJsonSupport {

  private val logger = Logger[HintServlet]
  private val requestLogger = Logger("request")
  private val dtoConverter = new DtoConverter(ontology)
  private val pipeline = new Pipeline(ontology, entryCache)
  private val executor = new Executor(ontology, entryCache, dbConnector)
  private val pending = new mutable.HashMap[String, CancellationSource]()

  protected implicit val jsonFormats: Formats = new DefaultFormats {
    override val typeHintFieldName = "type"
    override val typeHints = new TokenTypeHints
  } + LocalDateSerializer

  before() {
    contentType = formats("json")
  }

  post("/") {
    logger.info("request: " + request.body)
    val req = parsedBody.extract[HintRequestDto]
    logger.info("Request parsed into: " + req)
    requestLogger.info("Req [" + req.sessionId + "] " + req.tokens.map(t => t.text).mkString(" "))

    val cancellationSource: CancellationSource = pending.synchronized {
      // if something is running for this session - cancel it
      pending.get(req.sessionId).foreach(cs => {
        cs.cancel()
        pending.remove(req.sessionId)
      })
      val cs = new CancellationSource()
      pending.put(req.sessionId, cs)
      cs
    }

    try {
      val tokens = req.tokens.map(dtoConverter.fromDto)
      val res = pipeline
        .parse(req.sessionId, tokens, req.editedToken, cancellationSource.token)
        .map(parseResult => parseResult.tokens.map(dtoConverter.toDto))
        .map(ts => DtoHint(ts))
      //    logger.info(s"response: ${write(res)}")
      res
    }
    // todo - maybe return something specific in case of cancelled operation
    catch {
      case _:CancellationException => status = 204 // NO_CONTENT
    }
    finally {
      pending.synchronized {
        pending.get(req.sessionId).foreach(cs => {
          // remove "pending" entry only if it really corresponds to this request (it could have been displaced)
          if (cs eq cancellationSource)
            pending.remove(req.sessionId)
        })
      }
    }
  }

  post("/query") {
    logger.info("request for query: " + request.body)
    val req = parsedBody.extract[DtoQuery]
    logger.info("Request parsed into: " + req)
    requestLogger.info("Exec [?] " + req.tokens.map(t => t.text).mkString(" "))

    val tokens = req.tokens.map(dtoConverter.fromDto)
    val res = executor.execute(req.sessionId, tokens, req.modifiers)
    DtoQueryResult(
      res.title,
      res.chartType match {
        case ScalarChart => "scalar"
        case HorizontalBarChart => "hbc"
        case VerticalBarChart => "vbc"
        case TableChart => "table"
      },
      res.series.map(s => DtoSeriesInfo(s.title, dtoConverter.toDto(s.dataType), s.units)),
      res.data,
      res.modifiers.map(m => DtoQueryModifier(m.queryModifier.id, m.queryModifier.label, m.isActive))
    )
  }

  // this (hack?) is needed because errors which happen during response rendering are not handled by "error" handler
  protected override def renderUncaughtException(e: Throwable)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
    logger.error(e.getMessage, e)
    status = 500
  }

  error {
    case e: Throwable =>
      logger.error(e.getMessage, e)
      status = 500
//      format match {
//        case "json" | "xml" =>
//          JArray(List(("message" -> "Unknown error"): JValue))
//        case _ =>
//          "Unknown error"
//      }
  }

  private def login= {
     var referer = request.getHeader("Referer")
     if (referer == null) referer = ""
     if (referer.indexOf("?") >= 0 && referer.indexOf("?") < referer.length -1)
       referer.substring(referer.indexOf("?")+1)
     else
       "?"
  }
}
