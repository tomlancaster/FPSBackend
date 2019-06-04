import play.sbt.PlaySettings
import sbt.Keys._



scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  guice,
  evolutions, jdbc,
  "org.postgresql" % "postgresql" % "42.2.5",
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "org.joda" % "joda-convert" % "2.1.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "5.2",
  "com.netaporter" %% "scala-uri" % "0.4.16",
  "net.codingwell" %% "scala-guice" % "4.2.1",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.pac4j" %% "play-pac4j" % "7.0.1",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.1.1" % Test,
  "io.gatling" % "gatling-test-framework" % "3.0.1.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test
)

lazy val GatlingTest = config("gatling") extend Test

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayService, PlayLayoutPlugin, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """familyphotosharing""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.grubby.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.grubby.binders._"

