package domain
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder

final case class IdToken(
    acr: Option[String],
    at_hash: Option[String],
    auth_time: Option[Long],
    azp: Option[String],
    email_verified: Option[Boolean],
    exp: Option[Long],
    family_name: Option[String],
    given_name: Option[String],
    iat: Long,
    iss: Option[String],
    jti: Option[String],
    name: Option[String],
    preferred_username: Option[String],
    session_state: Option[String],
    sid: Option[String],
    sub: Option[String],
    typ: Option[String],
    aud: Option[String]
)

object IdToken {
  implicit val decoder: Decoder[IdToken] = deriveDecoder[IdToken]
}
