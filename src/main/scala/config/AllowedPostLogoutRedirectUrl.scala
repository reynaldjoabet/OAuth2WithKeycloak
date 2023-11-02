package config
import ciris._

final case class AllowedPostLogoutRedirectUrl(value: String)

object AllowedPostLogoutRedirectUrl {
  val postLogoutRedirectUrl: ConfigValue[Effect, AllowedPostLogoutRedirectUrl] =
    env("REDIRECT_URL")
      .as[String]
      .default(
        "http://localhost:8097/"
      )
      .map(AllowedPostLogoutRedirectUrl(_))
}
