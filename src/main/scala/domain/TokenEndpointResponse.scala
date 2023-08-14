package domain
import io.circe.generic.semiauto.deriveCodec
import io.circe.generic.semiauto.deriveEncoder
import io.circe.Codec
import io.circe.Decoder
final case class TokenEndpointResponse(
    accessToken: String,
    expiresIn: Int,
    refreshToken: String,
    refreshExpiresIn: Int,
    tokenType: String,
    idToken: String,
    notBeforePolicy: Int,
    sessionState: String,
    scope: String
)

object TokenEndpointResponse {
  // implicit val codec: Codec[TokenEndpointResponse] =
  // deriveCodec[TokenEndpointResponse]

  val encoder = deriveEncoder[TokenEndpointResponse]
  val decoder = Decoder.instance { h =>
    for {
      accessToken <- h.get[String]("access_token")
      expiresIn <- h.get[Int]("expires_in")
      refreshToken <- h.get[String]("refresh_token")
      refreshExpiresIn <- h.get[Int]("refresh_expires_in")
      tokenType <- h.get[String]("token_type")
      idToken <- h.get[String]("id_token")
      notBeforePolicy <- h.get[Int]("not-before-policy")
      sessionState <- h.get[String]("session_state")
      scope <- h.get[String]("scope")

    } yield TokenEndpointResponse(
      accessToken,
      expiresIn,
      refreshToken,
      refreshExpiresIn,
      tokenType,
      idToken,
      notBeforePolicy,
      sessionState,
      scope
    )
  }

  implicit val codec = Codec.from(decoder, encoder)

}
