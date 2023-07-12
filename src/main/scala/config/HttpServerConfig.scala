package config
//import com.comcast.ip4s.{ Host, Port }
import com.comcast.ip4s._
import ciris._
import cats.syntax.all._

final case class HttpServerConfig(
    host: Option[Host],
    port: Option[Port]
)
object HttpServerConfig {
  val serverConfig: ConfigValue[Effect, HttpServerConfig] =
    (env("HOST").default("localhost"), env("PORT").as[String].default("8090"))
      .parMapN((host, port) =>
        HttpServerConfig(Host.fromString(port), Port.fromString(port))
      )
}
