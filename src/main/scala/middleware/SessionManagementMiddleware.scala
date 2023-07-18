package middleware

import cats.effect.kernel.Async
import org.http4s.Request
import org.http4s.server.HttpMiddleware
import cats.data.Kleisli
import scala.collection.concurrent.TrieMap
import cats.syntax.all._
final case class SessionManagementMiddleware[F[_]: Async]()

object SessionManagementMiddleware {
  def apply[F[_]: Async]: HttpMiddleware[F] = service =>
    Kleisli { request: Request[F] =>
      ???
    }
}
