package routes
//import org.http4s.client.dsl._
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
import domain._
import org.http4s.AuthedRoutes
import org.http4s.Status
import org.http4s.ResponseCookie
import org.http4s.Uri
import org.http4s.HttpDate
import org.http4s.SameSite
import scala.concurrent.duration._
import org.http4s.Request
import org.http4s.Credentials
import org.http4s.AuthScheme
import errors.InvalidState
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.Base64
import io.circe.parser
import io.circe.syntax._
import io.circe.Json
import scala.util.Success
import scala.util.Try
import scala.util.Failure
import org.http4s.server.AuthMiddleware
import cats.data.OptionT
import cats.effect.std.UUIDGen
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import io.circe.Decoder
import org.http4s.Challenge
import org.http4s.HttpRoutes
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.metrics.prometheus.PrometheusExportService
import cats.effect.kernel.Resource
import org.http4s.server.middleware.Metrics
import org.http4s.server.Router

final case class AuthenticationRoutes[F[_]: Async: Console](
    clientService: ClientService[F],
    userSessionService: UserSessionService[F],
    tokenService: TokenService[F],
    uuidGen: UUIDGen[F]
)(implicit val logger: Logger[F])
    extends Http4sDsl[F] {
  object CodeParam extends OptionalQueryParamDecoderMatcher[String]("code")
  object StateParam extends OptionalQueryParamDecoderMatcher[String]("state")

  object FrontendRedirectParam extends OptionalQueryParamDecoderMatcher[String]("redirect")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "login" :? FrontendRedirectParam(redirect) =>
      (getFrontendRedirectUrl(redirect) product isValidSession(request)).flatMap {
        case (frontendRedirect, isValid) =>
          if (isValid)
            Ok()
          // TemporaryRedirect(
          // Location(Uri.unsafeFromString(frontendRedirect.value))
          // )
          else
            clientService
              .makeKeycloakRedirect(frontendRedirect.value)
              .flatMap(url => TemporaryRedirect(Location(url)))
      }
        .recoverWith(_ => InternalServerError("Failed to generate redirect url."))

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
            tokenEndpointResponse <- clientService
                                       .fetchBearerToken(code)
            // .flatTap(token=>Console[F].println(token))
            idToken <- extractToken[IdToken](
                         tokenEndpointResponse.idToken
                       ) // flatTap(Console[F].println)
            // parse access token and id token
            newSessionId <-
              uuidGen.randomUUID.flatMap(uuid => generateSessionId(uuid.toString()))
            // idToken <- extractIdToken(tokenEndpointResponse.id_token)
            userInfo <- clientService
                          .getUserInfo(tokenEndpointResponse.accessToken)
            // .flatTap(Console[F].println)
            session <- Async[F].delay(
                         UserSession(
                           newSessionId,
                           userInfo.sub,
                           List.empty[String],
                           tokenEndpointResponse.accessToken,
                           tokenEndpointResponse.refreshToken,
                           tokenEndpointResponse.refreshExpiresIn,
                           tokenEndpointResponse.idToken
                         )
                       )
            _ <- userSessionService.setUserSession(newSessionId, session)
            response <- TemporaryRedirect(
                          Location(Uri.unsafeFromString(frontendUrl))
                        )
                          .map(_.addCookie(createCookie(newSessionId)))

          } yield response).recoverWith {
            case _: InvalidState =>
              BadRequest("Invalid 'state' query parameter")
            case error: Throwable =>
              InternalServerError("Faile to log user in")

          }

      }
  }

  val routes2 = AuthedRoutes.of[UserSession, F] {
    case POST -> Root / "token" as userSession =>
      userSessionService
        .getFreshAccessToken(userSession.sessionId)
        .flatMap {
          case Some(token) => Ok(AccessTokenResponse(token))
          case None        => BadRequest()
        }
        .recoverWith { case err => InternalServerError(err.toString()) }
    // case request @ GET -> Root / "sso-logout" http://localhost:8081/sso-logout
    case request @ POST -> Root / "logout" as userSession =>
      (for {
        _ <- clientService.endUserSessionOnKeycloak(userSession)
        _ <- (userSessionService.deleteUserSession(
               userSession.sessionId
             ) *> logger.info(
               s"Successfully logged out user ${userSession.userId}"
             ))
      } yield ())
        .flatMap(_ => Ok())
        .map(_.removeCookie(COOKIE_NAME))
        .uncancelable // By default the logout endpoint will redirect back to the root of the application after logout is done.
    // The logout endpoint will trigger revocation of the user’s refresh token (if present).

    case req @ GET -> Root / "user" as userSession =>
      // if there is no current session, the user endpoint will return a 401 status code
      // if there is a valid session, the user endpoint returns a user object
      (for {
        userInfo <- clientService
                      .getUserInfo(userSession.accessToken)
      } yield userInfo).flatMap(Ok(_))

    case req @ POST -> Root / "backchannel" / "logout" as userSession =>
      // we add an implementation of the OpenID Connect back-channel notification endpoint to overcome
      // the restrictions of third party cookies in front-channel notification in modern browsers.

      req.req.as[BackChannelLogoutRequest].flatMap { backChannelLogoutRequest =>
        for {
          logoutToken <- extractToken[LogoutToken](
                           backChannelLogoutRequest.logoutToken
                         )
        } yield ()

        Ok("")
      }

  }

  def createCookie(sessionId: String): ResponseCookie =
    ResponseCookie(
      name = COOKIE_NAME,
      content = sessionId,
      expires = None,
      maxAge = Some(1.hour.toSeconds),
      path = Some("/"),
      sameSite = Some(SameSite.Strict),
      secure = true,
      httpOnly = true,
      // domain = None
      domain = Some("localhost")
    )

  // TODO: incorporate cookie signing.
  // __Host-XSESSION
  private val COOKIE_NAME = "XSESSION"

  def getFrontendUrlFromState(state: String): F[Option[String]] =
    tokenService
      .getState(state)
      .<*(tokenService.deleteState(state).void)

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
        .map(_.isDefined)
        // .flatMap(userSession => Async[F].delay(userSession.isDefined))
        .recoverWith(_ => Async[F].delay(false))
    }
  }

  private def generateSessionId(token: String): F[String] =
    Async[F].delay {
      val mac = MessageDigest.getInstance("SHA3-512")
      val digest = mac.digest(token.getBytes())
      // a 160-bit (20 byte) random value that is then URL-safe base64-encoded
      // byte[] buffer = new byte[20];
      Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
      // Base64.getEncoder().encodeToString(digest)
    }

  // A regex that defines the JWT pattern and allows us to
  // extract the header, claims and signature
  private val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r

  // Splits a JWT into it's 3 component parts
  private val splitToken: String => Try[String] =
    (token: String) =>
      token match {
        case jwtRegex(header, body, sig) =>
          Success(new String(Base64.getDecoder.decode(body)))
        case _ =>
          Failure(new Exception("Token does not match the correct pattern"))
      }

  // As the header and claims data are base64-encoded, this function
  // decodes those elements
  // private val decodeElements: Try[(String, String, String)] => Try[(String, String, String)] =
  // (data: Try[(String, String, String)]) =>
  // data map { case (header, body, sig) =>
  // (Base64.getDecoderdecodeS(header), JwtBase64.decodeString(body), sig)
  // }

  private def extractToken[A](jwtToken: String)(implicit d: Decoder[A]): F[A] =
    Async[F]
      .fromTry(splitToken(jwtToken))
      .flatMap { jwtBody =>
        for {
          jsonToken <- Async[F].fromEither(parser.parse(jwtBody))
          token <- Async[F].fromEither(jsonToken.as[A])
        } yield token
      }
      // .flatMap(t=>Async[F].fromEither(parser.parse(t)))
      // .flatMap(j=>Async[F].fromEither(j.as[A]))
      .flatTap(t => Console[F].println(t))

  // infers properly thanks to kind projector compiler plugin
  // Kleisli[[X]=>>OptionT[F,X],Request[F],UserSession] in scala3
  /**
   * Int has has a kind of A, String has a kind of A. Kleisli[F[_],A,B] this is a type constructor that takes a type
   * constructor(s). A higher -kinder type Types like List that take one argument are called type constructors. List is
   * a type constructor, a first order kinded type, Function1 like Either and Map are binary type constructors( takes
   * two aruguments) Function1[A,B], Either[A,B] and Map[A,B] are proper types. A=>? is a type constructor that when
   * you apply a proper type, say X, you get back a function A=>X . F[_] is a type constructor and F[A] is a proper
   * type .Higher-kinded types are type constructors that take other types( or even other type constructors) as
   * parameter
   *
   * trait Functor[F[_]] expects a type constructor with one parameter Functor[List], Functor[Option] but not
   * Functor[Map] as Map takes 2 parameters(Map[K,V]) while the type parameter to Functor experts one Partial
   * applications of type constructors We can use type aliases to partially apply a type constructor and so adapt the
   * kind of the type to be used type IntkeyMap[A]=Map[Int,A] Functor[IntKeyMap]// works now. { type T[Y] = OptionT[F,
   * Y] } defines a structural type denoted by {} with a type alias inside. { type T[Y] = OptionT[F, Y] })#T defines an
   * anonymous type, inside of which is defined a type alia and then accessing the type alias with the # syntax # is
   * type projection, used to reference the type member T of the structural type { type T[Y] = OptionT[F, Y] })#T is a
   * type lamda
   * @return
   */
  // the F[_] becomes OptionT[F,?] and F[B] becomes OptionT[F,UserSession]
  def authUser1: Kleisli[({ type Y[X] = OptionT[F, X] })#Y, Request[F], UserSession] =
    Kleisli { request: Request[F] =>
      extractRequestAuth(request) match {
        case None => OptionT.none[F, UserSession]
        case Some(sessionId) =>
          OptionT(userSessionService.getUserSession(sessionId))
      }
    }

  // def authUser3: Kleisli[({ type T[Y] = OptionT[F, Y] })#T, Request[F], UserSession] = new Kleisli[({ type T[Y] = OptionT[F, Y] })#T, Request[F], UserSession]{
//override val run: Request[F] => OptionT[F,UserSession] = ???
  // }
  def authUser: Kleisli[OptionT[F, *], Request[F], UserSession] = Kleisli { request: Request[F] =>
    extractRequestAuth(request) match {
      case None => OptionT.none[F, UserSession]
      case Some(sessionId) =>
        OptionT(userSessionService.getUserSession(sessionId))
    }
  }
// path dependent type is a subset of type projection
// you can pass a path dependent type where a projection is needed
//we need to introduce a type alias and type alias can only be declared in a trait, class, method or an object definition.
//Unfortunately we cannot define that somewhere “in between” the def and the return type but what we can do is define a structural type in the generic parameter list.
// we define the type alias in the anonymous structural type and then we refer to it with the # operator

  // Kleisli if a wrapper for a function from A=>F[B]
  // with OptionT[F,*] as F, then it becomes A=>OptionT[F,B]
  // val mn= new Kleisli[OptionT[F,*],Request[F],UserSession]{
  // override val run: Request[F] => OptionT[F,UserSession] = ???
  // }
  val sessionMiddleware: AuthMiddleware[F, UserSession] =
    AuthMiddleware[F, UserSession](authUser1)

  val sessionMiddleware1 =
    AuthMiddleware.noSpider[F, UserSession](authUser1, onFailure3)
  val onFailure: AuthedRoutes[Error, F] =
    Kleisli { request =>
      OptionT.liftF(
        Unauthorized.apply(
          `WWW-Authenticate`(Challenge("Bearer", "issuer.toString"))
          // errorBody(request.context)
        )
      )
    }

  def onFailure3: Request[F] => F[Response[F]] = (request: Request[F]) =>
    Unauthorized.apply(
      `WWW-Authenticate`(Challenge("Bearer", "issuer.toString")),
      request.body
    )

  private def prometheusReporter(
      httpRoutes: HttpRoutes[F]
  ): Resource[F, HttpRoutes[F]] =
    (for {
      prometheusExportService <- PrometheusExportService.build[F]
      prometheusMetricsOps <- Prometheus.metricsOps[F](
                                prometheusExportService.collectorRegistry,
                                "server"
                              )
    } yield Metrics(prometheusMetricsOps)(httpRoutes) <+> prometheusExportService.routes)

//he extension method can be imported via import cats.implicits._.

//It comes from SemigroupK, which can be derived for any OptionT given a Monad for F
  val combinedRoutes = Router(
    "/api" -> routes,
    "/auth" -> sessionMiddleware(routes2)
  )
  val allRoutes: Resource[F, HttpRoutes[F]] = prometheusReporter(combinedRoutes)
}
