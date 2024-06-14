package config

import cats.syntax.all._

import ciris._

final case class ClientCredentials(
  clientId: Secret[String],
  clientSecret: Secret[String]
)

object ClientCredentials {

  private[this] val clientSecret: ConfigValue[Effect, Secret[String]] =
    env("CLIENT_SECRET")
      .as[String]
      .default(
        "hePDotavZz8HLN9lGqUbNFVRhqQlBXTr"
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
