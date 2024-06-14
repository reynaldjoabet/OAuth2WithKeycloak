package middleware

import cats.data._
import cats.implicits._
import cats.Monad

import org.http4s._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware

// Reponses object contains Ops classes for different response types
//Responses trait contains implicit conversions of the different response types
object TokenAuthenticationMiddleware {

  def apply[F[_]: Monad, T](
    onFailure: Request[F] => F[Response[F]],
    tokenRepo: TokenRepository[F, T]
  ): AuthMiddleware[F, T] = AuthMiddleware.noSpider(authenticateUser(tokenRepo), onFailure)

  // Middleware that extracts the token from the Authorization header and authenticates the user
  private def authenticateUser[F[_]: Monad, T](
    tokenRepo: TokenRepository[F, T]
  ): Kleisli[OptionT[F, *], Request[F], T] = Kleisli { req: Request[F] =>
    req.headers.get[Authorization] match {
      case Some(Authorization(Credentials.Token(AuthScheme.Bearer, token))) =>
        // check user in database
        OptionT(tokenRepo.findUserByToken(token))

      case _ => OptionT.none[F, T]
    }

  }

  trait TokenRepository[F[_], T] {
    def findUserByToken(token: String): F[Option[T]]
  }

  case class User(id: Long, username: String, roles: Set[String])

  private def authenticateUser1[F[_]: Monad](
    tokenRepo: TokenRepository[F, User],
    roles: Set[String]
  ): Kleisli[OptionT[F, *], Request[F], User] = Kleisli { req: Request[F] =>
    req.headers.get[Authorization] match {
      case Some(Authorization(Credentials.Token(AuthScheme.Bearer, token))) =>
        // check user in database
        OptionT
          .liftF(tokenRepo.findUserByToken(token))
          .flatMap {
            case Some(user) =>
              if (roles.subsetOf(user.roles)) OptionT.pure[F](user) else OptionT.none[F, User]
            case None => OptionT.none[F, User]
          }

      case _ => OptionT.none[F, User]
    }

  }

  private def authenticateUser2[F[_]: Monad](
    tokenRepo: TokenRepository[F, User],
    roles: Set[String]
  ): Kleisli[OptionT[F, *], Request[F], User] = Kleisli { req: Request[F] =>
    req
      .headers
      .get[Authorization]
      .collect { case Authorization(Credentials.Token(AuthScheme.Bearer, token)) =>
        OptionT
          .liftF(tokenRepo.findUserByToken(token))
          .flatMap {
            case Some(user) =>
              if (roles.subsetOf(user.roles)) OptionT.pure[F](user) else OptionT.none[F, User]
            case None => OptionT.none[F, User]
          }
      }
      .getOrElse(OptionT.none[F, User])
  }

  // Example usage:
  def authMiddleware[F[_]: Monad](tokenRepo: TokenRepository[F, User]): AuthMiddleware[F, User] =
    AuthMiddleware(authenticateUser1[F](tokenRepo, Set("ADMIN")))

  // Example usage:
  def authMiddleware2[F[_]: Monad](tokenRepo: TokenRepository[F, User]): AuthMiddleware[F, User] =
    AuthMiddleware(authenticateUser2[F](tokenRepo, Set("ADMIN")))

  // access control is the selective restriction of access to a system
  // In computer security, a permission is related to a resource

  // a permission is a declaration of an action that can be executed on a resource
  // without the resource, the permissions have no use

  // to express the ability to perform an action on a resource, we use priviledges
  // Priviledges are assigned permissions

  // Resources expose permissions and users have priviledges
  // can be assigned to users and applications
  // this application could then access the protected resource on behalf of a user( delegated authorization)

  // scopes enable a mechanism to define what an application can do on behalf of the user
  // applications are authorized to  access resources on behalf of the user, if the user does not have the priviledges, the application can't exercise it.
  // This means on the resource side, the user's privildges must be checked even in the presnce of granted scopes
  // In a delegated authorization scenario, the application may act on bealf of the user even when the user is not logged in
  // if a user no longer has the priviledges between the time of their consent and the exercised by the application, then the application must be prevented from exercising its delegated access

  // When you assign a permission to a user, you are granting them a privilege.If you assign a user the permission to read a document, you are granting them the privilege to read that document.

  // The entity that protects the resource is responsible for restricting access to it, i.e., it is doing access control.
//scopes are permissions of a resource that the application wants to exercise on behalf of the user.
//This means that the application can't do more than the user can do.

}
