package config
import ciris._
import cats.syntax.all._

final case class AllowedPostLogoutRedirectUrl(value: String)

object AllowedPostLogoutRedirectUrl {
  val postLogoutRedirectUrl: ConfigValue[Effect, AllowedPostLogoutRedirectUrl] =
    env("REDIRECT_URL")
      .as[String]
      .default(
        "http://localhost:8097/callback"
      )
      .map(AllowedPostLogoutRedirectUrl(_))
}
