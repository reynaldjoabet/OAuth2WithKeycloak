package domain
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder

final case class IDToken(
    acr: Option[String],
    address: Option[Address],
    at_hash: Option[String],
    auth_time: Option[Int],
    azp: Option[String],
    birthdate: Option[String],
    c_hash: Option[String],
    category: Option[String],
    claims_locales: Option[String],
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
    nbf: Option[String],
    nickname: Option[String],
    nonce: Option[String],
    otherClaims: Option[Map[String, String]],
    phone_number: Option[String],
    phone_number_verified: Option[Boolean],
    picture: Option[String],
    preferred_username: Option[String],
    profile: Option[String],
    s_hash: Option[String],
    session_state: Option[String],
    sid: Option[String],
    sub: Option[String],
    typ: Option[String],
    update_at: Option[Int],
    website: Option[String],
    zoneinfo: Option[String]
)

object IDToken {
  implicit val decoder: Decoder[IDToken] = deriveDecoder[IDToken]
}
