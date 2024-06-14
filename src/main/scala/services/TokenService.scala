package services

import scala.concurrent.duration.FiniteDuration

import cats.effect.kernel.Async

import dev.profunktor.redis4cats.RedisCommands

trait TokenService[F[_]] {

  def setState(
    ket: String,
    value: String,
    expiration: FiniteDuration
  ): F[Unit]

  def deleteState(sessionId: String): F[Long]
  def getState(ket: String): F[Option[String]]

}

object TokenService {

  def make[F[_]: Async](redisCommands: RedisCommands[F, String, String]) =
    new TokenService[F] {

      override def setState(
        ket: String,
        value: String,
        expiration: FiniteDuration
      ): F[Unit] =
        redisCommands.setEx(ket, value, expiration)

      override def deleteState(sessionId: String): F[Long] =
        redisCommands.del(sessionId)

      override def getState(ket: String): F[Option[String]] =
        redisCommands.get(ket)

    }

}
