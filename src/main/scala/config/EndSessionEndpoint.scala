package config
import ciris._

final case class EndSessionEndpoint(value: String)
object EndSessionEndpoint {
  val endSessionEndpoint: ConfigValue[Effect, EndSessionEndpoint] =
    env("END_SESSION_ENDPOINT")
      .as[String]
      .default(
        "http://localhost:8080/realms/FlashPay/protocol/openid-connect/logout"
      )
      .map(EndSessionEndpoint(_))
//single logout (or single sign-out):

}
