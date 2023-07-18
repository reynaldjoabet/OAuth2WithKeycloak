ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

val http4sVersion = "0.23.16"
val cirisVersion = "3.1.0"
val circeVersion = "0.14.5"
val catsEffectVersion = "3.4.8"
val fs2Version = "3.7.0"
val redis4catsVersion = "1.4.3"
def circe(artifact: String): ModuleID =
  "io.circe" %% s"circe-$artifact" % circeVersion
def ciris(artifact: String): ModuleID = "is.cir" %% artifact % cirisVersion
def http4s(artifact: String): ModuleID =
  "org.http4s" %% s"http4s-$artifact" % http4sVersion
val circeGenericExtras = circe("generic-extras")
val circeCore = circe("core")
val circeGeneric = circe("generic")
val cireParser = "io.circe" %% "circe-parser" % circeVersion
val retry = "com.github.cb372" %% "cats-retry" % "3.1.0"
val cirisCore = ciris("ciris")
val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
val fs2 = "co.fs2" %% "fs2-core" % fs2Version
val redis4cats = "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion
val http4sDsl = http4s("dsl")
val http4sServer = http4s("ember-server")
val http4sClient = http4s("ember-client")
val http4sCirce = http4s("circe")

val logbackVersion = "1.4.7"

val logback = "ch.qos.logback" % "logback-classic" % logbackVersion

lazy val root = (project in file("."))
  .settings(
    name := "OAuth2WithKeycloak",
    libraryDependencies ++= Seq(
      cirisCore,
      http4sDsl,
      http4sServer,
      http4sClient,
      http4sCirce,
      circeCore,
      circeGeneric,
      logback,
      catsEffect,
      fs2,
      retry,
      redis4cats,
      cireParser
    )
  )

fork := true
