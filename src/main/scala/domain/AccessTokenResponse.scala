package domain

import io.circe.generic.semiauto.deriveCodec
import io.circe.Decoder._

final case class AccessTokenResponse(accessToken: String)

object AccessTokenResponse {
  implicit val codec = deriveCodec[AccessTokenResponse]
}
