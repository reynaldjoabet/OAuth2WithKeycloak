import java.sql

import cats.data._
import cats.Monad
import cats.Applicative
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.server._
import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

object AuthExample {

  // Define a case class to represent your user
  case class User(id: Int, username: String)

  // A simple authentication function that validates a JWT token
  def authenticateToken(token: String): Option[User] = {
    ???
  }

  val g = List(Some(2)).sequence[Option, Int]
  // Middleware that extracts the token from the Authorization header and authenticates the user
  def tokenAuthenticationMiddleware[F[_]: Monad]: AuthMiddleware[F, User] = AuthMiddleware[F, User] {
    Kleisli { req: Request[F] =>
      req.headers.get[Authorization] match {
        case Some(Authorization(Credentials.Token(AuthScheme.Bearer, token))) =>
          ??? // OptionT(authenticateToken(token))
        case None                                                            => OptionT.none[F, User]
        case Some(Authorization(Credentials.Token(AuthScheme.Basic, token))) => ???

        case Some(Authorization(Credentials.Token(AuthScheme.OAuth, token))) => ???

        case Some(Authorization(Credentials.Token(AuthScheme.Digest, token))) => ???

        case Some(Authorization(Credentials.AuthParams(AuthScheme.Bearer, params))) =>
          ??? // OptionT(authenticateToken(token))

        case Some(Authorization(Credentials.AuthParams(AuthScheme.Basic, params))) => ???

        case Some(Authorization(Credentials.AuthParams(AuthScheme.OAuth, params))) => ???

        case Some(Authorization(Credentials.AuthParams(AuthScheme.Digest, params))) => ???
      }
    }
  }

  // Define your routes
  val service = AuthedRoutes.of[User, IO] {
    case GET -> Root / "secured" as user => Ok(s"Hello, ${user.username}!")
  }

  // Create a service with the authentication middleware
  val securedService: HttpRoutes[IO] = tokenAuthenticationMiddleware[IO].apply(service)

  def main(args: Array[String]): Unit = {
    // Start your server with securedService
  }

}

sealed abstract class Roles

object Roles {
  case object Admin extends Roles
  case object User extends Roles
  case object Editor extends Roles
}
val defaultRoles = Set("Admin", "User", "Editor")

def hasRoles(roles: Set[Roles]): Boolean = roles.map(_.toString()).subsetOf(defaultRoles)
val admin = Roles.Admin.toString()

hasRoles(Set(Roles.Admin, Roles.User))

val program3: ConnectionIO[(Int, Double)] =
  for {
    a <- sql"select 42".query[Int].unique
    b <- sql"select random()".query[Double].unique
  } yield (a, b)
//Out of the box doobie provides an interpreter from its free monads to Kleisli[M, Foo, ?] given Async[M].

import cats.data.Kleisli
import doobie.free.connection.ConnectionOp
import java.sql.Connection
import cats._
val interpreter: ~>[ConnectionOp, Kleisli[IO, Connection, *]] =
  KleisliInterpreter[IO](LogHandler.noop).ConnectionInterpreter
program3.foldMap(interpreter)

import org.http4s.server.middleware.VirtualHost

Set("ADMIN").map(_.toLowerCase()).subsetOf(Set("Admin").map(_.toLowerCase()))

val s1 = Set(1, 2, 3, 4)

val s2 = Set(1, 2, 3, 4, 5)

s1.subsetOf(s2)

s2.subsetOf(s1)

doobie.util.ExecutionContexts.fixedThreadPool[IO](34)

doobie.util.ExecutionContexts.cachedThreadPool[IO]

sealed abstract class Permission

object Permission {
  case object ADMIN_READ extends Permission
  case object ADMIN_CREATE extends Permission
  case object ADMIN_DELETE extends Permission
  case object ADMIN_UPDATE extends Permission

  case object MANAGER_READ extends Permission
  case object MANAGER_CREATE extends Permission
  case object MANAGER_DELETE extends Permission
  case object MANAGER_UPDATE extends Permission

}

import Permission._

sealed abstract class Role

object Role {
  case class USER() extends Role

case class EMPLOYEE() extends Role
  case class ADMIN(
      permissions: Set[Permission] = Set(
        Permission.ADMIN_CREATE,
        Permission.ADMIN_READ,
        Permission.ADMIN_UPDATE,
        Permission.ADMIN_DELETE,
        Permission.MANAGER_CREATE,
        Permission.MANAGER_UPDATE,
        Permission.MANAGER_READ,
        Permission.MANAGER_DELETE
      )
  ) extends Role
  case class MANAGER(
      permissions: Set[Permission] =
        Set(Permission.MANAGER_CREATE, Permission.MANAGER_UPDATE, Permission.MANAGER_READ, Permission.MANAGER_DELETE)
  ) extends Role
}

Permission.ADMIN_CREATE.toString().toLowerCase()


Role.ADMIN().toString()

Role.ADMIN().toString().toLowerCase()
