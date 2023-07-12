package config
import ciris._
final case class FrontendUrl(
    value: String
)

object FrontendUrl {
  val frontendUrl: ConfigValue[Effect, FrontendUrl] =
    env("REDIRECT_URL")
      .as[String]
      .default(
        "http://localhost:3000"
      )
      .map(FrontendUrl(_))
}
