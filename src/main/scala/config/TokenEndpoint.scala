package config
import ciris._
final case class TokenEndpoint(value: String)

object TokenEndpoint {
  val tokenEndpoint: ConfigValue[Effect, TokenEndpoint] =
    env("TOKEN_ENDPOINT")
      .as[String]
      .default(
        "http://localhost:8080/realms/FlashPay/protocol/openid-connect/token"
      )
      .map(TokenEndpoint(_))

}
