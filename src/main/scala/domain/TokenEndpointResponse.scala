package domain
import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
final case class TokenEndpointResponse(
    access_token: String,
    expires_in: Int,
    refresh_token: String,
    refresh_expires_in: Int,
    token_type: String,
    id_token: String,
    `not-before-policy`: Int,
    session_state: String,
    scope: String
)

object TokenEndpointResponse {
  implicit val codec: Codec[TokenEndpointResponse] =
    deriveCodec[TokenEndpointResponse]
}
