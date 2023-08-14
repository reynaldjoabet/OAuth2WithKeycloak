package services
import cats.syntax.all._
import domain._
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres.implicits._
import cats.effect.kernel.MonadCancelThrow
trait DatabaseService[F[_]] {

  /** Create!
    */
  def createUser(userId: String): F[Unit]

  def createUserSession(
      sessionId: String,
      refreshToken: String,
      userId: String
  ): F[Unit]

  /** Read!
    */
  def getUsers: F[List[User]]

  def getUserById(userId: String): F[Option[User]]

  def getUserSession(sessionId: String): F[Option[UserSession]]

  /** Delete!
    *
    * Returns whether a record was deleted.
    */
  def deleteUserSession(sessionId: String): F[Boolean]

  def createOrUpdateUser(
      sessionId: String,
      refreshToken: String,
      userId: String
  ): F[Unit]
}

object DatabaseService {

  def make[F[_]: MonadCancelThrow](xa: Transactor[F]) = new DatabaseService[F] {
    override def createUser(userId: String): F[Unit] =
      sql"INSERT into user (userId) VALUES($userId) ON CONFLICT DO NOTHING".update.run
        // .withUniqueGeneratedKeys[User]("user_id","created_at")
        .transact(xa)
        .as(())

    def createUser1(userId: String): F[User] =
      sql"INSERT into user (userId) VALUES($userId) ON CONFLICT DO NOTHING".update
        .withUniqueGeneratedKeys[User]("user_id", "created_at")
        .transact(xa)

    override def createUserSession(
        sessionId: String,
        refreshToken: String,
        userId: String
    ): F[Unit] =
      sql"INSERT into user_session (session_id,refresh_token,user_id) VALUES($sessionId,$refreshToken,$userId) ON CONFLICT DO NOTHING".update.run
        // .withUniqueGeneratedKeys[User]("user_id","created_at")
        .transact(xa)
        .as(())

    def createUserSession1(
        sessionId: String,
        refreshToken: String,
        userId: String
    ): F[UserSession] =
      sql"INSERT into user_session (session_id,refresh_token,user_id) VALUES($sessionId,$refreshToken,$userId) ON CONFLICT DO NOTHING".update
        .withUniqueGeneratedKeys[UserSession](
          "session_id",
          "refresh_token",
          "user_id",
          "created_at"
        )
        .transact(xa)
    override def createOrUpdateUser(
        sessionId: String,
        refreshToken: String,
        userId: String
    ): F[Unit] =
      createUser(userId) *> createUserSession(sessionId, refreshToken, userId)
    override def getUsers: F[List[User]] =
      sql"SELECT * from user"
        .query[User]
        .to[List]
        .transact(xa)

    override def getUserById(userId: String): F[Option[User]] =
      sql"SELECT * from user where userid= $userId"
        .query[User]
        .option
        .transact(xa)

    override def getUserSession(sessionId: String): F[Option[UserSession]] =
      sql"SELECT * from user_session where sessionid= $sessionId"
        .query[UserSession]
        .option
        .transact(xa)

    override def deleteUserSession(sessionId: String): F[Boolean] =
      sql"DELETE from user_session where sessionid= $sessionId".update.run
        .transact(xa)
        .map(_ > 0)

  }
}
