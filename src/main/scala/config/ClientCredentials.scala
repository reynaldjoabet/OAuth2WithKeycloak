package config
import ciris._
import cats.syntax.all._

final case class ClientCredentials(
    clientId: Secret[String],
    clientSecret: Secret[String]
)

object ClientCredentials {
  private[this] val clientSecret: ConfigValue[Effect, Secret[String]] =
    env("CLIENT_SECRET")
      .as[String]
      .default(
        "04aEfZgi1n7R1s7VHnFid4kb458JGKSs"
      )
      .secret

  private[this] val clientId: ConfigValue[Effect, Secret[String]] =
    env("CLIENT_ID")
      .as[String]
      .default(
        "AuthenticationApp"
      )
      .secret

  def credentials: ConfigValue[Effect, ClientCredentials] =
    (clientId, clientSecret).parMapN(ClientCredentials(_, _))

}
