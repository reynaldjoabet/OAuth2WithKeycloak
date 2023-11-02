package domain
import io.circe.Decoder
final case class BackChannelLogoutRequest(logoutToken: String)

object BackChannelLogoutRequest {

  implicit val decoder: Decoder[BackChannelLogoutRequest] =
    Decoder.instance[BackChannelLogoutRequest] { h =>
      for {
        logoutToken <- h.get[String]("logout_token")
      } yield BackChannelLogoutRequest(logoutToken)
    }

}
