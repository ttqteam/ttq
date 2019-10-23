import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import ttq.cache.{EntryCacheImpl, EntryCacheLoader}
import ttq.common.DbConnector
import ttq.config.Config
import ttq.dsl.Ontology
import ttq.dsl.yaml.YamlParser
import ttq.ontology.OntologyParser
import ttq.web.HintServlet

import scala.util.Try

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) = {
    val config = Config.config
    val ontology = OntologyParser.getFinalOntology(loadOntology(config.ontologyFile))
    val dbConnector = new DbConnector(config.db)
    val cacheLoader = new EntryCacheLoader(ontology, dbConnector)
    val enrtyCache = new EntryCacheImpl(ontology, config.cacheFile)
    cacheLoader.fillCache(enrtyCache)

    context mount (new HintServlet(ontology, enrtyCache, dbConnector), "/hint/*")
  }

  private def loadOntology(ontologyFile: String) =
  {
    val b = scala.io.Source.fromFile(ontologyFile)
    try {
      val yaml = b.mkString
      val cls = YamlParser.readOntologyClass(yaml)
      val o = new Ontology
      o.addClass(cls)
      o
    } finally {
      Try {b.close()}
    }
  }
}
