package domain

import io.circe.generic.semiauto.deriveCodec
import io.circe.generic.semiauto.deriveEncoder
import io.circe.Codec
import io.circe.Decoder

final case class UserInfoResponse(
  sub: String,
  familyName: String,
  givenName: String,
  preferredUsername: String,
  name: String,
  emailVerified: Boolean
)

object UserInfoResponse {

  // implicit val codec: Codec[UserInfoResponse] = deriveCodec[UserInfoResponse]
  val encoder = deriveEncoder[UserInfoResponse]

  val decoder = Decoder.instance { h =>
    for {
      sub               <- h.get[String]("sub")
      familyName        <- h.get[String]("family_name")
      givenName         <- h.get[String]("given_name")
      preferredUsername <- h.get[String]("preferred_username")
      name              <- h.get[String]("name")
      emailVerified     <- h.get[Boolean]("email_verified")
    } yield UserInfoResponse(
      sub,
      familyName,
      givenName,
      preferredUsername,
      name,
      emailVerified
    )
  }

  implicit val codec = Codec.from(decoder, encoder)

}
