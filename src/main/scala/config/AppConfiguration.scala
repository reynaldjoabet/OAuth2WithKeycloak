package config

import cats.effect.kernel.Async
import cats.implicits._

import ciris.ConfigValue
import ciris.Effect

final case class AppConfiguration private (
  flyway: FlywayConfiguration,
  serverConfig: ServerConfiguration
)

object AppConfiguration {

  def appConfig: ConfigValue[Effect, AppConfiguration] =
    for {
      serverConfig <- ServerConfiguration.serverConfig
      flywayConfig <- FlywayConfiguration.flywayConfig
    } yield AppConfiguration(flywayConfig, serverConfig)

}
