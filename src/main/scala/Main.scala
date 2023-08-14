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
import cats.effect.std.UUIDGen
import cats.effect.std.Supervisor
import cats.effect.std.syntax.supervisor
import org.http4s.server.middleware.ResponseLogger
import org.http4s.server.middleware.CSRF
import org.http4s.Uri
import org.http4s.server.middleware.CORS
import org.typelevel.ci._
import org.http4s.Method._
object Main extends IOApp {
//implicit val loggerName=LoggerName("name")
  override protected def blockedThreadDetectionEnabled = true
  private implicit val logger = Slf4jLogger.getLogger[IO]

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"
    )

  val headerName = "X-Csrf-Token" // default
  val cookieName = "csrf-token" // default

  private val corsService = CORS.policy
    .withAllowOriginHost(Set("http://localhost:3000"))
    .withAllowMethodsIn(Set(POST, PUT, GET, DELETE))
    .withAllowCredentials(
      false
    ) // set to true for csrf// The default behavior of cross-origin resource requests is for
    // requests to be passed without credentials like cookies and the Authorization header
    .withAllowHeadersIn(Set(ci"X-Csrf-Token", ci"Content-Type"))

  def csrfService = CSRF
    .withGeneratedKey[IO, IO](request =>
      CSRF.defaultOriginCheck(request, "localhost", Uri.Scheme.http, None)
    )
    .map(builder =>
      builder
        // .withCookieName(cookieName)
        .withCookieDomain(Some("localhost"))
        .withCookiePath(Some("/"))
        // .withCookieSecure(true)// defaults to false
        // .withCookieHttpOnly(false) //defaults to true
        .build
        .validate()
    )
    .toResource

  override def run(args: List[String]): IO[ExitCode] =
    Supervisor[IO].use { supervisor =>
      // supervisor.supervise
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
        csrfMiddleware <- csrfService

        uuidGen = UUIDGen[IO]
        tokenService = TokenService.make(redisCommandsUtf8)
        clientService = ClientService.make(client, tokenService, uuidGen)
        userSessionService = UserSessionService.make(
          redisCommands,
          clientService
        )

        routes = AuthenticationRoutes(
          clientService,
          userSessionService,
          tokenService,
          uuidGen
        )

        _ <- EmberServerBuilder
          .default[IO]
          .withHttpApp(
            csrfMiddleware(
              ResponseLogger.httpApp(true, true, _ => false)(
                RequestLogger.httpApp(true, true, _ => false)(
                  routes.allRoutes.orNotFound
                )
              )
            )
          )
          .withPort(port"8097")
          .withHost(host"127.0.0.1")
          .build
          .evalTap(showEmberBanner[IO](_))

      } yield ()).useForever.as(ExitCode.Success)
    }

}
