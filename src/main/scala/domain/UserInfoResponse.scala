package domain
import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
final case class UserInfoResponse(
    sub: String,
    family_name: String,
    given_name: String,
    preferred_username: String,
    name: String,
    email_verified: Boolean
)

object UserInfoResponse {
  implicit val codec: Codec[UserInfoResponse] = deriveCodec[UserInfoResponse]
}
