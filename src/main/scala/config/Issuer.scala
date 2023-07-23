package config
import ciris._
final case class Issuer(value: String)

object Issuer {
  val issuer: ConfigValue[Effect, Issuer] =
    env("ISSUER")
      .as[String]
      .default(
        "http://localhost:8080/realms/FlashPay"
      )
      .map(Issuer(_))

}
