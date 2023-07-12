package routes
import org.http4s.client.dsl._
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import cats.effect.kernel.Async
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import config._
import cats.effect.IO
import org.typelevel.log4cats.Logger
import cats.syntax.all._
import cats.effect.std.Console
import scala.concurrent.duration._
import cats.effect.syntax.all._
import cats.data.Kleisli
import org.http4s.Response
import org.http4s.circe._
import client._
import services._
import domain.AccessTokenResponse
import domain._
import domain.AccessTokenResponse._
import org.http4s.AuthedRoutes
import org.http4s.Status
import org.http4s.ResponseCookie
import org.http4s.Uri
import org.http4s.HttpDate
import java.time.Instant
import org.http4s.SameSite
import scala.concurrent.duration._
import org.http4s.Request
import org.http4s.Credentials
import org.http4s.AuthScheme
import cats.effect.syntax.all._
import cats.effect.syntax.all._
import errors.InvalidState
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.util.UUID

final case class AuthenticationRoutes[F[_]: Async: Console](
    clientService: ClientService[F],
    userSessionService: UserSessionService[F],
    redisService: RedisService[F]
)(implicit val logger: Logger[F])
    extends Http4sDsl[F] {
  object CodeParam extends OptionalQueryParamDecoderMatcher[String]("code")
  object StateParam extends OptionalQueryParamDecoderMatcher[String]("state")
  object RedirectParam
      extends OptionalQueryParamDecoderMatcher[String]("redirect")
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "login" :? RedirectParam(redirect) =>
      (getFrontendRedirectUrl(redirect) product isValidSession(request))
        .flatMap { case (frontendRedirect, isValid) =>
          if (isValid)
            TemporaryRedirect(
              Location(Uri.unsafeFromString(frontendRedirect.value))
            )
          else
            clientService
              .makeKeycloakRedirect(frontendRedirect.value)
              .flatMap(url => TemporaryRedirect(Location(url)))
        }
        .recoverWith(_ =>
          InternalServerError("Failed to generate redirect url.")
        )

    case request @ GET -> Root / "callback" :? CodeParam(code) :? StateParam(
          state
        ) =>
      code -> state match {
        case (None, _) => BadRequest("Missing 'code' query parameter")
        case (_, None) => BadRequest("Missing 'state' query parameter")
        case (Some(code), Some(state)) =>
          (for {
            frontendUrlOption <- getFrontendUrlFromState(state)
            frontendUrl <- Async[F].fromOption(
              frontendUrlOption,
              InvalidState()
            )
            tokenEndpointResponse <- clientService.fetchBearerToken(code)
            // parse access token and id token
            newSessionId <- Async[F]
              .delay(UUID.randomUUID())
              .map(
                _.toString()
              ) // generateSessionId(tokenEndpointResponse.token_type)
            response <- TemporaryRedirect(
              Location(Uri.unsafeFromString(frontendUrl))
            )
              .map(_.addCookie(createCookie(newSessionId)))

          } yield response)
            .recoverWith(_ => InternalServerError("Faile to log user in"))

      }
  }

  val routes2 = AuthedRoutes.of[UserSession, F] {
    case request @ POST -> Root / "token" as userSession =>
      userSessionService
        .getFreshAccessToken(userSession.sessionId)
        .flatMap {
          case Some(token) => Ok(AccessTokenResponse(Some(token)))
          case None        => BadRequest()
        }

    case request @ POST -> Root / "logout" as userSession =>
      (for {
        _ <- clientService.endUserSessionOnKeycloack()
        _ <- (userSessionService.deleteUserSession(
          userSession.sessionId
        ) *> logger.info(
          s"Successfully logged out user ${"userSession.userId"}"
        ))
      } yield ())
        .flatMap(_ => Ok().map(_.removeCookie(COOKIE_NAME)))
        .uncancelable

  }

  def createCookie(sessionId: String): ResponseCookie =
    ResponseCookie(
      name = COOKIE_NAME,
      content = sessionId,
      expires = None,
      maxAge = Some(1.hour.toSeconds),
      path = None,
      sameSite = Some(SameSite.Strict),
      secure = true,
      httpOnly = true,
      domain = Some("localhost")
    )
  // TODO: incorporate cookie signing.
  private val COOKIE_NAME = "XSESSION"

  def getFrontendUrlFromState(state: String): F[Option[String]] =
    redisService
      .get[String, String](state)
      .<*(redisService.delete(state).void)

  // If no redirect is provided, use the frontend url from config.
  def getFrontendRedirectUrl(redirectMaybe: Option[String]): F[FrontendUrl] =
    FrontendUrl.frontendUrl.load[F]

  private def extractRequestAuth(request: Request[F]) = {
    val cookie =
      request.cookies.filter(_.name == COOKIE_NAME).headOption.map(_.content)
    val token = request.headers.get[Authorization].collect {
      case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token
    }
    cookie.orElse(token)
  }

  def isValidSession(request: Request[F]): F[Boolean] = {
    extractRequestAuth(request).fold(Async[F].delay(false)) { sessionId =>
      userSessionService
        .getUserSession(sessionId)
        .flatMap(userSession => Async[F].delay(userSession.isDefined))
        .recoverWith(_ => Async[F].delay(false))
    }
  }

  private def generateSessionId(token: String): F[String] =
    Async[F].delay {
      val mac = MessageDigest.getInstance("SHA3-512")
      val digest = mac.digest(token.getBytes())
      new String(digest, StandardCharsets.US_ASCII)
    }
}
