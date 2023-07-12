package services
//import cats.syntax.all._
import scala.concurrent.duration.Duration
trait RedisService[F[_]] {
  def set[K, V](ket: K, value: V, expiration: Option[Duration]): F[Boolean]

  def get[K, V](key: K): F[Option[V]]

  def delete[K](key: K): F[Boolean]
}
