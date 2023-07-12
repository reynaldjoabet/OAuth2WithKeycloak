package middleware

import cats.effect.kernel.Async
import org.http4s.Request
import org.http4s.server.HttpMiddleware
import cats.data.Kleisli
import scala.collection.concurrent.TrieMap
import cats.syntax.all._
final case class SessionManagementMiddleware[F[_]: Async]() {
  private val cache = new TrieMap[Long, Int]
  def get(orderId: Long): F[Option[Int]] =
    cache.get(orderId).pure[F]

  def delete(orderId: Long): F[Option[Int]] =
    cache.remove(orderId).pure[F]
}
object SessionManagementMiddleware {
  def apply[F[_]: Async]: HttpMiddleware[F] = service =>
    Kleisli { request: Request[F] =>
      ???
    }
}
