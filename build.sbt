ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

val http4sVersion     = "0.23.16"
val cirisVersion      = "3.1.0"
val circeVersion      = "0.14.5"
val catsEffectVersion = "3.4.8"
val fs2Version        = "3.7.0"
val redis4catsVersion = "1.4.3"
val flywayVersion     = "9.21.0"
val postgresVersion   = "42.5.4"
val doobieVersion     = "1.0.0-RC4"
val logbackVersion    = "1.4.7"
val pureConfigVersion = "0.17.4"

def kamon(artifact: String) = "io.kamon" %% s"kamon-$artifact" % "2.6.1"
val kamonCore               = kamon("core")
val kamonHttp4s             = kamon("http4s-0.23")
val kamonPrometheus         = kamon("prometheus")
val kamonZipkin             = kamon("zipkin")
val kamonJaeger             = kamon("jaeger")

def circe(artifact: String): ModuleID =
  "io.circe" %% s"circe-$artifact" % circeVersion

def ciris(artifact: String): ModuleID = "is.cir" %% artifact % cirisVersion

def http4s(artifact: String): ModuleID =
  "org.http4s" %% s"http4s-$artifact" % http4sVersion

val prometheusMetrics = "org.http4s" %% "http4s-prometheus-metrics" % "0.24.6"

val circeGenericExtras = circe("generic-extras")
val circeCore          = circe("core")
val circeGeneric       = circe("generic")
val cireParser         = "io.circe"         %% "circe-parser"        % circeVersion
val retry              = "com.github.cb372" %% "cats-retry"          % "3.1.0"
val cirisCore          = ciris("ciris")
val catsEffect         = "org.typelevel"    %% "cats-effect"         % catsEffectVersion
val fs2                = "co.fs2"           %% "fs2-core"            % fs2Version
val redis4cats         = "dev.profunktor"   %% "redis4cats-effects"  % redis4catsVersion
val redis4catsLog4cats = "dev.profunktor"   %% "redis4cats-log4cats" % redis4catsVersion
val http4sDsl          = http4s("dsl")
val http4sServer       = http4s("ember-server")
val http4sClient       = http4s("ember-client")
//val blazeClient= ???
//val blazeServer= "org.http4s" %% "http4s-blaze-server" % "0.23.15"
val http4sCirce = http4s("circe")

val doobie_hikari   = "org.tpolecat"  %% "doobie-hikari"   % doobieVersion
val postgres        = "org.postgresql" % "postgresql"      % postgresVersion
val flyway          = "org.flywaydb"   % "flyway-core"     % flywayVersion
val doobie          = "org.tpolecat"  %% "doobie-core"     % doobieVersion
val doobie_postgres = "org.tpolecat"  %% "doobie-postgres" % doobieVersion
val logback         = "ch.qos.logback" % "logback-classic" % logbackVersion
//val cirisHocon= "lt.dvim.ciris-hocon" %% "ciris-hocon" % "1.1.0"

val pureConfig = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion

lazy val root = (project in file(".")).settings(
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
    redis4catsLog4cats,
    cireParser,
    doobie_hikari,
    flyway,
    doobie,
    doobie_postgres,
    postgres,
    // cirisHocon,
    pureConfig,
    kamonCore,
    kamonHttp4s,
    kamonPrometheus,
    kamonZipkin,
    kamonJaeger,
    prometheusMetrics
  )
) //.settings(commonSettings)
// by default sbt run runs the program in the same JVM as sbt
//in order to run the program in a different JVM, we add the following
//fork in run := true
fork := true
scalacOptions ++= Seq(
  "-deprecation", // Warning and location for usages of deprecated
  "-encoding",
  "utf-8", // Specify character encoding used by source
  "-feature",
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:infer-any",             // A type argument is inferred to be `Any`.
  "-unchecked",
  "-explaintypes",          // Explain type errors in more detail.
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

ThisBuild / run / fork := true

ThisBuild / fork in Runtime := true

javaOptions ++= Seq(
  // "-J-XX:ActiveProcessorCount=4", // Overrides the automatic detection mechanism of the JVM that doesn't work very well in k8s.
//    "-J-XX:MaxRAMPercentage=80.0",  // 80% * 1280Mi = 1024Mi (See https://github.com/conduktor/conduktor-devtools-builds/pull/96/files#diff-1c0a26888454bc51fc9423622b5d4ee82456b0420f169518a371f3f0e23d443cR67-R70)
  // "-J-XX:+ExitOnOutOfMemoryError",
  // "-J-XX:+HeapDumpOnOutOfMemoryError",
  // "-J-XshowSettings:system",      // https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness#recent_changes_in_openjdk_s_container_awareness_code
  "-Dfile.encoding=UTF-8"
)
Compile / run / mainClass := Some("Main")
//mainClass in (Compile ,run) := Some("Main")
// src/main/scala/Main.scala => Some("Main")
// src/main/scala/com/baeldung/packaging/Main.scala => Some("com.baeldung.packaging.Main")
// java -jar ./target/scala-2.13/oauth2withkeycloak_2.13-0.1.0-SNAPSHOT.jar   to run

Compile / run / fork := true
scalacOptions        += "-target:17" // ensures the Scala compiler generates bytecode optimized for the Java 17 virtual machine

//We can also set the soruce and target compatibility for the Java compiler by configuring the JavaOptions in build.sbt

// javaOptions ++= Seq(
//   "-source",
//   "17",
//   "target",
//   "17"
// )
semanticdbEnabled := true
