name := """tx-verify"""
organization := "io.ergopool"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"

val circeVersion = "0.12.3"

//libraryDependencies += guice
libraryDependencies += play.sbt.PlayImport.guice

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

javaOptions in Test += "-Dconfig.file=conf/test.conf"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat

  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.ergopool.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.ergopool.binders._"
