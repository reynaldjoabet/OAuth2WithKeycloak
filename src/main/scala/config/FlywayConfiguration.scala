package config

import cats.effect.kernel.Async
import cats.implicits._
import ciris._
final case class FlywayConfiguration private (url: String, username: String, password: String)

object FlywayConfiguration {
  // private val hocon = hoconAt("flyway")

  def flywayConfig: ConfigValue[Effect, FlywayConfiguration] = ???

}
