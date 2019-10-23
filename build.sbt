name := "ontology"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions := Seq("-Xfatal-warnings")

val ScalatraVersion = "2.6.3"

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-json" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "org.scalamock" %% "scalamock" % "4.1.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "com.github.pureconfig" %% "pureconfig" % "0.11.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.9.v20180320" % "container;compile",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.json4s" %% "json4s-native" % "3.5.2",
  "org.json4s" %% "json4s-jackson" % "3.5.2",
  "org.json4s" %% "json4s-ext" % "3.5.2",
  "com.h2database" % "h2" % "1.4.+",
  "org.scalatest" %% "scalatest" % "3.0.1",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "org.springframework" % "spring-core" % "2.5.6",
  "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.1.50",
  "org.yaml" % "snakeyaml" % "1.24"
)

enablePlugins(ScalatraPlugin)

// sbt-native-packager
import NativePackagerHelper._
enablePlugins(JavaAppPackaging)
mainClass in Compile := Some("ttq.demo.web.JettyLauncher")
mappings in Universal ++= directory("src/main/webapp")
mappings in Universal ++= directory("data")
