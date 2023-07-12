package repository
import domain._
import cats.effect.kernel.Ref
import cats.syntax.all._
import scala.collection.concurrent.TrieMap
import cats.effect.kernel.Async
import services.RedisService
import client.ClientService

sealed trait SessionManagementRepository[F[_]] {

  def getUserSession(sessionId: String): F[Option[UserSession]]
  def getFreshAccessToken(sessionId: String): F[Option[String]]
  def refreshAndGetUserSession(sessionId: String): F[Option[UserSession]]
  def deleteUserSession(sessionId: String): F[Boolean]
  // def isRateLimited(userId: String): F[Boolean]
  // INSERT INTO employees (id, name, email)
//VALUES (2, ‘Dennis’, ‘dennisp@weyland.corp’)
//ON CONFLICT (id) DO UPDATE;
}

object SessionManagementRepository {
  def make[F[_]: Async](
      store: Ref[F, TrieMap[String, String]],
      redisService: RedisService[F],
      clientService: ClientService[F]
  ) = new SessionManagementRepository[F] {
    override def getUserSession(sessionId: String): F[Option[UserSession]] =
      redisService.get(sessionId)

    override def getFreshAccessToken(sessionId: String): F[Option[String]] =
      retrieveSession(sessionId).map(_.map(_.accessToken))

    override def refreshAndGetUserSession(
        sessionId: String
    ): F[Option[UserSession]] =
      deleteUserSession(sessionId) *> getUserSession(sessionId)
    override def deleteUserSession(sessionId: String): F[Boolean] =
      redisService.delete(sessionId)

    private def retrieveSession(sessionId: String): F[Option[UserSession]] =
      getUserSession(sessionId)
        .flatMap {
          case Some(session) =>
            clientService
              .fetchRefreshAccessToken(session.refreshToken)
              .map(resp =>
                Some(
                  UserSession(
                    session.sessionId,
                    session.userId,
                    Set.empty[String],
                    resp.access_token,
                    refreshToken = resp.refresh_token,
                    resp.expires_in
                  )
                )
              )
          case None => Async[F].pure(None)
        }
  }

}
