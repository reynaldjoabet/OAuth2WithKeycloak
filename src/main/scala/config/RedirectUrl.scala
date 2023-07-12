package config
import ciris._
import cats.syntax.all._

final case class RedirectUrl(value: String)

object RedirectUrl {
  val redirectUrl: ConfigValue[Effect, RedirectUrl] =
    env("REDIRECT_URL")
      .as[String]
      .default(
        "http://localhost:8081/callback"
      )
      .map(RedirectUrl(_))
}
