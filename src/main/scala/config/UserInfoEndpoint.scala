package config
import ciris._

final case class UserInfoEndpoint(value: String)

object UserInfoEndpoint {
  val userInfoEndpoint: ConfigValue[Effect, UserInfoEndpoint] =
    env("USER_INFO_ENDPOINT")
      .as[String]
      .default(
        "http://localhost:8080/realms/FlashPay/protocol/openid-connect/userinfo"
      )
      .map(UserInfoEndpoint(_))

}
