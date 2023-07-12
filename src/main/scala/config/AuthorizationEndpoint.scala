package config
import ciris._
import cats.syntax.all._
final case class AuthorizationEndpoint(value: String)

object AuthorizationEndpoint {
  val authorizationEndpoint: ConfigValue[Effect, AuthorizationEndpoint] =
    env("AUTHORIZATION_ENDPOINT")
      .as[String]
      .default(
        "http://localhost:8080/realms/FlashPay/protocol/openid-connect/auth"
      )
      .map(AuthorizationEndpoint(_))
}
