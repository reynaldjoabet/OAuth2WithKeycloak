package middleware
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization
import org.http4s._
import cats.Monad
import cats.data._
import cats.implicits._

object CookieAuthenticationMiddleware {
  case class User(id: Long, username: String, roles: Set[String])
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

// Example usage:
  def authMiddleware[F[_]: Monad](tokenRepo: CookieRepository[F, User]): AuthMiddleware[F, User] =
    AuthMiddleware(authenticateUser[F](tokenRepo, Set("ADMIN")))

// Example usage:
  def authMiddleware2[F[_]: Monad](tokenRepo: CookieRepository[F, User]): AuthMiddleware[F, User] =
    AuthMiddleware(authenticateUser2[F](tokenRepo, Set("ADMIN")))

  trait CookieRepository[F[_], T] {
    def findUserByCookie(token: String): F[Option[T]]
  }

  final abstract class RedisImplementation[F[_]: Monad, T] extends CookieRepository[F, T] {
    override def findUserByCookie(token: String): F[Option[T]]

  }
}
