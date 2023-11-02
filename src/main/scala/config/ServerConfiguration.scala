package config

///import model._
import cats.implicits._
import cats.effect.IO
import cats.effect.kernel.Async
import ciris._
final case class ServerConfiguration private (host: String, port: Int)

object ServerConfiguration {

  def serverConfig: ConfigValue[Effect, ServerConfiguration] = ???

}
