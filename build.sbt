import play.sbt.PlaySettings
import sbt.Keys._



scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  guice,
  evolutions, jdbc,
  "org.postgresql" % "postgresql" % "42.2.5",
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "org.joda" % "joda-convert" % "2.2.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
  "io.lemonlabs" %% "scala-uri" % "1.4.5",
  "net.codingwell" %% "scala-guice" % "4.2.3",
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.typesafe.akka" %% "akka-actor" % "2.5.22",
  "com.typesafe.akka" %% "akka-stream" % "2.5.22",
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-http-xml" % "10.1.8",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.0.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test
)

lazy val GatlingTest = config("gatling") extend Test

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayService, PlayLayoutPlugin)
  .settings(
    name := """familyphotosharing""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.grubby.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.grubby.binders._"

