package domain

import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder

final case class IdToken(
  acr: Option[String],
  atHash: Option[String],
  authTime: Option[Long],
  azp: Option[String],
  emailVerified: Option[Boolean],
  exp: Option[Long],
  familyName: Option[String],
  givenName: Option[String],
  iat: Long,
  iss: Option[String],
  jti: Option[String],
  name: Option[String],
  preferredUsername: Option[String],
  sessionState: Option[String],
  sid: Option[String],
  sub: Option[String],
  typ: Option[String],
  aud: Option[String]
)

object IdToken {

  // implicit val decoder: Decoder[IdToken] = deriveDecoder[IdToken]
  implicit val decoder = Decoder.instance { h =>
    for {
      acr               <- h.get[Option[String]]("acr")
      atHash            <- h.get[Option[String]]("at_hash")
      authTime          <- h.get[Option[Long]]("auth_time")
      azp               <- h.get[Option[String]]("azp")
      emailVerified     <- h.get[Option[Boolean]]("email_verified")
      exp               <- h.get[Option[Long]]("exp")
      familyName        <- h.get[Option[String]]("family_name")
      givenName         <- h.get[Option[String]]("given_name")
      iat               <- h.get[Long]("iat")
      iss               <- h.get[Option[String]]("iss")
      jti               <- h.get[Option[String]]("jti")
      name              <- h.get[Option[String]]("name")
      preferredUsername <- h.get[Option[String]]("preferred_username")
      sessionState      <- h.get[Option[String]]("session_state")
      sid               <- h.get[Option[String]]("sid")
      sub               <- h.get[Option[String]]("sub")
      typ               <- h.get[Option[String]]("typ")
      aud               <- h.get[Option[String]]("aud")
    } yield IdToken(
      acr,
      atHash,
      authTime,
      azp,
      emailVerified,
      exp,
      familyName,
      givenName,
      iat,
      iss,
      jti,
      name,
      preferredUsername,
      sessionState,
      sid,
      sub,
      typ,
      aud
    )
  }

}
