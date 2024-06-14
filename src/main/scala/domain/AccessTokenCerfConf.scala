package domain

import io.circe.generic.semiauto.deriveCodec

final case class AccessTokenCerfConf(
  `x5t#S256`: Option[String]
)

object AccessTokenCerfConf {
  implicit val codec = deriveCodec[AccessTokenCerfConf]
}
