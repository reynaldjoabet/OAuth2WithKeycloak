ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

val http4sVersion = "0.23.16"
val cirisVersion = "3.1.0"
val circeVersion = "0.14.5"
val catsEffectVersion = "3.4.8"
val fs2Version = "3.7.0"
val redis4catsVersion = "1.4.3"
val flywayVersion = "9.21.0"
val postgresVersion = "42.5.4"
val doobieVersion = "1.0.0-RC4"
val logbackVersion = "1.4.7"

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

val doobie_hikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
val postgres = "org.postgresql" % "postgresql" % postgresVersion
val flyway = "org.flywaydb" % "flyway-core" % flywayVersion
val doobie = "org.tpolecat" %% "doobie-core" % doobieVersion
val doobie_postgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
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
      cireParser,
      doobie_hikari,
      flyway,
      doobie,
      doobie_postgres,
      postgres
    )
  ) //.settings(commonSettings)

fork := true
scalacOptions ++= Seq(
  "-deprecation", // Warning and location for usages of deprecated
  "-encoding",
  "utf-8", // Specify character encoding used by source
  "-feature",
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:infer-any", // A type argument is inferred to be `Any`.
  "-unchecked",
  "-explaintypes", // Explain type errors in more detail.
  "-Ywarn-unused:implicits" // An implicit parameter is unused
  // "-Ywarn-unused:imports",   // An import selector is not referenced.
  // "-Ywarn-unused:params",  // A value parameter is unused.
  // "-Ywarn-unused:patvars",   // A variable bound in a pattern is unused.
  // "-Ywarn-unused:privates"  // A private member is unused
)
//scalacOptions += "-language:higherKinds"
addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)

run / fork := true
