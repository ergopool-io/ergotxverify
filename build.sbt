name := """tx-verify"""
organization := "io.ergopool"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"

val circeVersion = "0.12.3"


libraryDependencies += guice

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-servlet" % "9.4.24.v20191120",
  "org.eclipse.jetty" % "jetty-server" % "9.4.24.v20191120"
)

libraryDependencies += "org.ergoplatform" % "ergo-wallet_2.12" % "3.2.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.ergopool.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.ergopool.binders._"
