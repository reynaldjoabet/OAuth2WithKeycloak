package middleware
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization
import org.http4s._
import cats.Monad
import cats.data._
import cats.implicits._

object CookieAuthenticationMiddleware {
  case class User(id: Long, username: String, roles: Set[String])
  case class UserWithScopes(id: Long, username: String, roles: Set[String], scopes: Set[String])

  case class UserWithPermissions(id: Long, username: String, roles: Set[String], permissions: Set[Permission])
  // Role based authorization checks:
//Are declarative and specify roles which the current user must be a member of to access the requested resource.
  private def authenticateUser2[F[_]: Monad](
      cookieRepo: CookieRepository[F, User],
      roles: Set[String]
  ): Kleisli[OptionT[F, *], Request[F], User] = Kleisli { req: Request[F] =>
    req
      .cookies
      .filter(_.name == "")
      .headOption
      .map(_.content)
      .collect {
        case cookie =>
          OptionT.liftF(cookieRepo.findUserByCookie(cookie)).flatMap {
            case Some(user) if roles.subsetOf(user.roles) => OptionT.pure[F](user)
            case _                                        => OptionT.none[F, User]
          }
      }
      .getOrElse(OptionT.none[F, User])
  }

  private def authenticateUser[F[_]: Monad](
      cookieRepo: CookieRepository[F, User],
      roles: Set[String]
  ): Kleisli[OptionT[F, *], Request[F], User] = Kleisli { req: Request[F] =>
    req.cookies.filter(_.name == "cookiename").headOption.map(_.content) match {
      case Some(cookie) =>
        OptionT.liftF(cookieRepo.findUserByCookie(cookie)).flatMap {
          case Some(user) if roles.subsetOf(user.roles) => OptionT.pure[F](user)
          case _                                        => OptionT.none[F, User]
        }
      case None => OptionT.none[F, User]
    }
  }

  // scoped based authorization
  // be aware that scopes are authorizing clients and not users in OAuth.
  // A scope allows the client to invoke the functionality associated with the scope and is unrelated to the user's permission to do so
  // This additional user centric authorization is application logic and not covered by OAuth, yet still possibly important to implement in your API.
  private def authenticateUser3[F[_]: Monad](
      cookieRepo: CookieRepository[F, UserWithScopes],
      requiredScopes: Set[String]
  ): Kleisli[OptionT[F, *], Request[F], UserWithScopes] = Kleisli { req: Request[F] =>
    req.cookies.filter(_.name == "cookiename").headOption.map(_.content) match {
      case Some(cookie) =>
        OptionT.liftF(cookieRepo.findUserByCookie(cookie)).flatMap {
          case Some(user) if requiredScopes.subsetOf(user.scopes) => OptionT.pure[F](user)
          case _                                                  => OptionT.none[F, UserWithScopes]
        }
      case None => OptionT.none[F, UserWithScopes]
    }
  }

  // Role and claim-based authorization is a crucial aspect of securing applications by allowing access control based on roles and specific claims assigned to users.
  private def authenticateUser4[F[_]: Monad](
      cookieRepo: CookieRepository[F, UserWithPermissions],
      requiredPermissions: Set[Permission]
  ): Kleisli[OptionT[F, *], Request[F], UserWithPermissions] = Kleisli { req: Request[F] =>
    req.cookies.filter(_.name == "cookiename").headOption.map(_.content) match {
      case Some(cookie) =>
        OptionT.liftF(cookieRepo.findUserByCookie(cookie)).flatMap {
          case Some(user) if requiredPermissions.subsetOf(user.permissions) => OptionT.pure[F](user)
          case _                                                            => OptionT.none[F, UserWithPermissions]
        }
      case None => OptionT.none[F, UserWithPermissions]
    }
  }

// Example usage:
  def authMiddleware[F[_]: Monad](tokenRepo: CookieRepository[F, User]): AuthMiddleware[F, User] =
    AuthMiddleware(authenticateUser[F](tokenRepo, Set("ADMIN")))

// Example usage:
  def authMiddleware2[F[_]: Monad](tokenRepo: CookieRepository[F, User]): AuthMiddleware[F, User] =
    AuthMiddleware(authenticateUser2[F](tokenRepo, Set("ADMIN")))

  // Example usage:
  def authMiddleware3[F[_]: Monad](tokenRepo: CookieRepository[F, UserWithScopes]): AuthMiddleware[F, UserWithScopes] =
    AuthMiddleware(
      authenticateUser3[F](tokenRepo, Set("admin:read", "account:update", "account:delete", "account:create"))
    )
  trait CookieRepository[F[_], T] {
    def findUserByCookie(token: String): F[Option[T]]
  }

  final abstract class RedisImplementation[F[_]: Monad, T] extends CookieRepository[F, T] {
    override def findUserByCookie(token: String): F[Option[T]]

  }
}
