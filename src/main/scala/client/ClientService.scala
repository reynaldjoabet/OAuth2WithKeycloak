package client
import org.http4s.client.Client
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import cats.effect.kernel.Async
import org.http4s.client.dsl.Http4sClientDsl
import io.circe.syntax._
import cats.syntax.all._
import org.http4s.client.middleware.RequestLogger
import retry.RetryPolicy
import org.http4s.Status
import cats.effect.syntax.all._
import java.util.UUID
import config._
import services.TokenService
import scala.concurrent.duration._
import org.http4s.circe.CirceEntityCodec._
import domain.TokenEndpointResponse
import org.http4s.client.middleware.ResponseLogger
import domain.UserSession
import config.AllowedPostLogoutRedirectUrl
import domain.UserInfoResponse

final case class ClientService[F[_]: Async](
    client: Client[F],
    tokenService: TokenService[F]
) extends Http4sClientDsl[F] {
// Store State -> Redirect URL in Redis.
  // On callback, Redirecturl will be retrieved from Redis.
  def makeKeycloakRedirect(frontendUrl: String): F[Uri] = for {
    credentials <- ClientCredentials.credentials.load[F]
    authorizationEndpoint <- AuthorizationEndpoint.authorizationEndpoint.load[F]
    redirectUrl <- RedirectUrl.redirectUrl.load[F]
    state <- Async[F].delay(UUID.randomUUID().toString())
    _ <- tokenService.setState(state, frontendUrl, 40.seconds)
    queryParams = Map(
      "response_type" -> "code",
      "client_id" -> credentials.clientId.value,
      "redirect_uri" -> redirectUrl.value,
      "scope" -> scopes,
      "state" -> state
    )
  } yield Uri
    .unsafeFromString(authorizationEndpoint.value)
    .withQueryParams(queryParams)

  def endUserSessionOnKeycloack(userSession: UserSession) = for {
    credentials <- ClientCredentials.credentials.load[F]
    endSessionEndpoint <- EndSessionEndpoint.endSessionEndpoint.load[F]
    postLogoutRedirectUrl <- AllowedPostLogoutRedirectUrl.postLogoutRedirectUrl
      .load[F]
    request = Method.POST(
      UrlForm(
        "refresh_token" -> userSession.refreshToken,
        "client_secret" -> credentials.clientSecret.value,
        "client_id" -> credentials.clientId.value
        // "post_logout_redirect_uri" -> postLogoutRedirectUrl.value // Allowed Logout URLs
      ),
      Uri.unsafeFromString(endSessionEndpoint.value),
      Authorization(
        Credentials.Token(AuthScheme.Bearer, userSession.accessToken)
      )
    )
    _ <- ResponseLogger(true, true)(client).expect[String](request)
  } yield ()

  def getUserInfo(accessToken: String): F[UserInfoResponse] = for {
    userInfoEndpoint <- UserInfoEndpoint.userInfoEndpoint.load[F]
    request = Method.GET(
      Uri.unsafeFromString(userInfoEndpoint.value),
      Authorization(
        Credentials.Token(AuthScheme.Bearer, accessToken)
      )
    )

    token <- client.expect[UserInfoResponse](request)
  } yield token

  def fetchBearerToken(authorizationCode: String): F[TokenEndpointResponse] =
    for {
      credentials <- ClientCredentials.credentials.load[F]
      tokenEndpoint <- TokenEndpoint.tokenEndpoint.load[F]
      redirectUrl <- RedirectUrl.redirectUrl.load[F]
      request = Method.POST(
        UrlForm(
          "client_id" -> credentials.clientId.value,
          "client_secret" -> credentials.clientSecret.value,
          "code" -> authorizationCode,
          "grant_type" -> "authorization_code",
          "redirect_uri" -> redirectUrl.value
        ),
        Uri.unsafeFromString(tokenEndpoint.value)
      )

      token <- client.expect[TokenEndpointResponse](request)
    } yield token

  def fetchNewAccessToken(refreshToken: String): F[TokenEndpointResponse] =
    for {
      tokenEndpoint <- TokenEndpoint.tokenEndpoint.load[F]
      credentials <- ClientCredentials.credentials.load[F]
      request = Method.POST(
        UrlForm(
          "client_id" -> credentials.clientId.value,
          "client_secret" -> credentials.clientSecret.value,
          "refresh_token" -> refreshToken,
          "grant_type" -> "refresh_token"
        ),
        Uri.unsafeFromString(tokenEndpoint.value)
      )

      token <- client.expect[TokenEndpointResponse](request)
    } yield token

  private val scopes =
    List("openid").mkString(" ")
}

object ClientService {
  def make[F[_]: Async](
      client: Client[F],
      tokenService: TokenService[F]
  ): ClientService[F] = ClientService(client, tokenService)
}
