package services
import domain._
import cats.effect.kernel.Ref
import cats.syntax.all._
import scala.collection.concurrent.TrieMap
import cats.effect.kernel.Async
import client.ClientService
import scala.concurrent.duration.FiniteDuration
import dev.profunktor.redis4cats.RedisCommands
import scala.concurrent.duration.Duration

sealed trait UserSessionService[F[_]] {

  def getUserSession(sessionId: String): F[Option[UserSession]]
  def getFreshAccessToken(sessionId: String): F[Option[String]]
  def refreshAndGetUserSession(sessionId: String): F[Option[UserSession]]
  def deleteUserSession(sessionId: String): F[Long]
  def setUserSession(
      ket: String,
      value: UserSession,
      expiration: Option[FiniteDuration] = None
  ): F[Unit]
  // def isRateLimited(userId: String): F[Boolean]
  // INSERT INTO employees (id, name, email)
//VALUES (2, ‘Dennis’, ‘dennisp@weyland.corp’)
//ON CONFLICT (id) DO UPDATE;
}

object UserSessionService {
  def make[F[_]: Async](
      redisCommands: RedisCommands[F, String, UserSession],
      clientService: ClientService[F]
  ) = new UserSessionService[F] {
    override def getUserSession(sessionId: String): F[Option[UserSession]] =
      redisCommands.get(sessionId)

    override def getFreshAccessToken(sessionId: String): F[Option[String]] =
      retrieveSession(sessionId).map(_.map(_.accessToken))

    override def refreshAndGetUserSession(
        sessionId: String
    ): F[Option[UserSession]] =
      deleteUserSession(sessionId) *> getUserSession(sessionId)

    override def deleteUserSession(sessionId: String): F[Long] =
      redisCommands.del(sessionId)

    private def retrieveSession(sessionId: String): F[Option[UserSession]] =
      getUserSession(sessionId)
        .flatMap {
          case Some(session) =>
            clientService
              .fetchNewAccessToken(session.refreshToken)
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

    override def setUserSession(
        ket: String,
        value: UserSession,
        expiration: Option[FiniteDuration] = None
    ): F[Unit] =
      expiration match {
        case None      => redisCommands.set(ket, value)
        case Some(exp) => redisCommands.setEx(ket, value, exp)
      }

  }

}
