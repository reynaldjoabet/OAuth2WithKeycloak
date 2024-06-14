package config

import cats.effect.kernel.Async
import cats.effect.IO
///import model._
import cats.implicits._

import ciris._

final case class ServerConfiguration private (host: String, port: Int)

object ServerConfiguration {

  def serverConfig: ConfigValue[Effect, ServerConfiguration] = ???

}
