package services

import java.time.Duration

import scala.concurrent.duration._

// import org.bitlap.zim.cache.{JavaDuration, RedisService}
// import org.bitlap.zim.cache.redis4cats.CatsRedisConfiguration.{redisHost, redisPort}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import io.circe._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.lettuce.core.{ClientOptions, TimeoutOptions}
import java.time.{Duration => JavaDuration}
import cats.effect.{IO, Resource}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effects._
import dev.profunktor.redis4cats
import dev.profunktor.redis4cats.log4cats.log4CatsInstance

import dev.profunktor.redis4cats.log4cats

trait RedisService[F[_]] {

  def getSets(k: String): F[List[String]]

  def removeSetValue(k: String, m: String): F[Long]

  def setSet(k: String, m: String): F[Long]

  def set[T](k: String, v: T, expireTime: JavaDuration = java.time.Duration.ofMinutes(30))(implicit
      encoder: Encoder[T]
  ): F[Boolean]

  def get[T](key: String)(implicit decoder: Decoder[T]): F[Option[T]]

  def exists(key: String): F[Boolean]

  def del(key: String): F[Boolean]

}

object CatsRedisServiceLive {
  lazy val redisHost: String = "host"
  lazy val redisPort: Int = 8080 // "port"
  private val stringCodec: RedisCodec[String, String] = RedisCodec.Utf8

  private val mkOpts: IO[ClientOptions] =
    IO {
      ClientOptions
        .builder()
        .autoReconnect(false)
        .pingBeforeActivateConnection(false)
        .timeoutOptions(
          TimeoutOptions
            .builder()
            .fixedTimeout(Duration.ofSeconds(10))
            .build()
        )
        .build()
    }

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val resource: Resource[IO, RedisCommands[IO, String, String]] =
    for {
      uri <- Resource.eval(RedisURI.make[IO](s"redis://$redisHost:$redisPort"))
      opts <- Resource.eval(mkOpts)
      client <- RedisClient[IO].custom(uri, opts)
      redis <- Redis[IO].fromClient(client, stringCodec)
    } yield redis

}

final case class CatsRedisServiceLive(redis: Resource[IO, RedisCommands[IO, String, String]])(implicit
    logger: Logger[IO]
) extends RedisService[IO] {

  override def getSets(k: String): IO[List[String]] =
    logger.info(s"Redis sMembers command: $k") *> redis.use { redis =>
      redis.sMembers(k).map(_.toList)
    }

  override def removeSetValue(k: String, m: String): IO[Long] =
    logger.info(s"Redis sRem command: $k, $m") *> redis.use { redis =>
      redis.sRem(k, m).map(_ => 1)
    }

  override def setSet(k: String, m: String): IO[Long] =
    logger.info(s"Redis sAdd command: $k, $m") *> redis.use { redis =>
      redis.sAdd(k, m)
    }

  override def set[T](k: String, v: T, expireTime: JavaDuration = java.time.Duration.ofMinutes(30))(implicit
      encoder: Encoder[T]
  ): IO[Boolean] =
    logger.info(s"Redis set command: $k, $v, $expireTime") *> redis.use { redis =>
      redis
        .set(k, v.asJson.noSpaces, SetArgs(SetArg.Existence.Nx, SetArg.Ttl.Ex(expireTime.getSeconds.seconds)))
        .map(_ => true)
    }

  override def get[T](key: String)(implicit decoder: Decoder[T]): IO[Option[T]] =
    logger.info(s"Redis get command: $key") *> redis.use { redis =>
      redis
        .get(key)
        .map {
          case Some(value) => decode(value).toOption
          case None        => None
        }
    }

  override def exists(key: String): IO[Boolean] =
    logger.info(s"Redis exists command: $key") *> redis.use { redis =>
      redis.exists(key)
    }

  override def del(key: String): IO[Boolean] =
    logger.info(s"Redis del command: $key") *> redis.use { redis =>
      redis.del(key).map(_ > 0)
    }
}
