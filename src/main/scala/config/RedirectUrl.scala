package config
import ciris._

final case class RedirectUrl(value: String)

object RedirectUrl {
  val redirectUrl: ConfigValue[Effect, RedirectUrl] =
    env("REDIRECT_URL")
      .as[String]
      .default(
        "https://localhost:8097/callback"
      )
      .map(RedirectUrl(_))
}
