import services.TokenService
import services.UserSessionService
import cats.effect.ExitCode
import cats.effect._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._
import org.http4s.server.defaults.Banner
import org.http4s.server.Server
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.LoggerName
import io.circe
import routes._
import services._
import org.http4s.server.middleware.RequestLogger
import cats.effect.std.Random
import org.http4s.ember.client.EmberClientBuilder
import client.ClientService
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.algebra
import dev.profunktor.redis4cats.data.RedisCodec.Utf8
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._
import domain.UserSession
import dev.profunktor.redis4cats.data
import org.http4s.server.AuthMiddleware
import cats.Functor

object Main extends IOApp {
//implicit val loggerName=LoggerName("name")

  Functor[({ type l[a] = Function1[Int, a] })#l]
  private implicit val logger = Slf4jLogger.getLogger[IO]

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"
    )

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      client <- EmberClientBuilder
        .default[IO]
        .build
      redisClient <- RedisClient[IO].from("redis://localhost")
      redisCommands <- Redis[IO].fromClient[String, UserSession](
        redisClient,
        UserSession.userSessionCodec
      )
      redisCommandsUtf8 <- Redis[IO].fromClient(
        redisClient,
        data.RedisCodec.Utf8
      )

      tokenService = TokenService.make(redisCommandsUtf8)
      clientService = ClientService.make(client, tokenService)
      userSessionService = UserSessionService.make(
        redisCommands,
        clientService
      )

      routes = AuthenticationRoutes(
        clientService,
        userSessionService,
        tokenService
      )

      _ <- EmberServerBuilder
        .default[IO]
        .withHttpApp(routes.allRoutes.orNotFound)
        .withPort(port"8097")
        .withHost(host"127.0.0.1")
        .build
        .evalTap(showEmberBanner[IO](_))

    } yield ()).useForever.as(ExitCode.Success)

}
