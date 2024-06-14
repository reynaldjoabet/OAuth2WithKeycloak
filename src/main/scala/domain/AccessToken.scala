package domain

import io.circe.generic.semiauto.deriveCodec

final case class AccessToken(
  acr: Option[String],
  address: Option[Address],
  `allowed-origns`: Option[List[String]],
  at_hash: Option[String],
  auth_time: Option[Int],
  authorization: Option[AccessTokenAuthorization],
  azp: Option[String],
  birthdate: Option[String],
  c_hash: Option[String],
  category: Option[Category],
  claims_locales: Option[String],
  cnf: Option[AccessTokenCerfConf],
  email: Option[String],
  email_verified: Option[Boolean],
  exp: Option[Int],
  family_name: Option[String],
  gender: Option[String],
  given_name: Option[String],
  iat: Option[Int],
  iss: Option[String],
  jti: Option[String],
  locale: Option[String],
  middle_name: Option[String],
  name: Option[String],
  nbf: Option[Int],
  nickname: Option[String],
  nonce: Option[String],
  otherClaims: Option[Map[String, String]],
  phone_number: Option[String],
  phone_number_verified: Option[Boolean],
  picture: Option[String],
  preferred_username: Option[String],
  profile: Option[String],
  realm_access: Option[AccessTokenAccess],
  s_hash: Option[String],
  scope: Option[String],
  session_state: Option[String],
  sid: Option[String],
  sub: Option[String],
  `trusted-certs`: Option[List[String]],
  typ: Option[String],
  update_at: Option[String],
  website: Option[String],
  zoneinfo: Option[String]
)

object AccessToken {
  implicit val codec = deriveCodec[AccessToken]
}
