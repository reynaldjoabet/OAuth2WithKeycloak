package domain

import io.circe.Decoder
final case class LogoutToken()

object LogoutToken {

  implicit val decoder: Decoder[LogoutToken] = Decoder[LogoutToken] { h =>
    for {
      logoutToken <- h.get[String]("logout_token")
    } yield LogoutToken()
  }

}
