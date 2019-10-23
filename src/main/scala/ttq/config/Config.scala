package ttq.config

import java.io.File

// implicits
import pureconfig.generic.auto._

case class Config(
  db: DbConfig,
  ontologyFile: String,
  cacheFile: String)


case object Config {
  lazy val config: Config = {
    val configFile = Option(System.getProperty("config.file")).getOrElse("conf/ttq.conf")
    pureconfig.loadConfig[Config](new File(configFile).toPath) match {
      case Right(c) => c
      case Left(err) =>
        val errTxt = err.toList
          .map(e => s"${e.description}" + e.location.map(l => s" Location: ${l.description}.").getOrElse(""))
          .mkString("\n")
        throw new IllegalArgumentException("Error reading configuration:\n" + errTxt)
    }
  }
}

