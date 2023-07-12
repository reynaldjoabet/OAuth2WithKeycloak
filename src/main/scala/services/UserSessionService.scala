package services
import domain._

trait UserSessionService[F[_]] {
  def getUserSession(sessionId: String): F[Option[UserSession]]
  def getFreshAccessToken(sessionId: String): F[Option[String]]
  def refreshAndGetUserSession(sessionId: String): F[Option[UserSession]]
  def deleteUserSession(sessionId: String): F[Boolean]
}
