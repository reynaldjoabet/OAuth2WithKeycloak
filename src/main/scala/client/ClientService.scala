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
import services.RedisService
import scala.concurrent.duration._
import org.http4s.circe.CirceEntityCodec._
import domain.TokenEndpointResponse

final case class ClientService[F[_]: Async](
    client: Client[F],
    redisService: RedisService[F]
) extends Http4sClientDsl[F] {
// Store State -> Redirect URL in Redis.
  // On callback, Redirecturl will be retrieved from Redis.
  def makeKeycloakRedirect(frontendUrl: String): F[Uri] = for {
    credentials <- ClientCredentials.credentials.load[F]
    authorizationEndpoint <- AuthorizationEndpoint.authorizationEndpoint.load[F]
    redirectUrl <- RedirectUrl.redirectUrl.load[F]
    state <- Async[F].delay(UUID.randomUUID().toString())
    _ <- redisService.set[String, String](state, frontendUrl, Some(40.seconds))
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

  def endUserSessionOnKeycloack() = for {
    credentials <- ClientCredentials.credentials.load[F]
    endSessionEndpoint <- EndSessionEndpoint.endSessionEndpoint.load[F]
    redirectUrl <- RedirectUrl.redirectUrl.load[F]
    request = Method.POST(
      UrlForm(
        "response_type" -> "code",
        "client_id" -> credentials.clientId.value,
        "redirect_uri" -> redirectUrl.value // Allowed Logout URLs
      ),
      Uri.unsafeFromString(endSessionEndpoint.value)
    )
    _ <- client.expect[String](request)
  } yield ()

  def getUserProfileInfo(accessToken: String) = for {
    credentials <- ClientCredentials.credentials.load[F]
    tokenEndpoint <- TokenEndpoint.tokenEndpoint.load[F]
    userInfoEndpoint <- UserInfoEndpoint.userInfoEndpoint.load[F]
    request = Method.POST(
      UrlForm(
        "client_id" -> credentials.clientId.value,
        "client_secret" -> credentials.clientSecret.value,
        // "code" -> authorizationCode,
        "grant_type" -> "authorization_code"
      ), // redirect,
      Uri.unsafeFromString(userInfoEndpoint.value)
    )

    token <- client.expect[String](request)
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
          "grant_type" -> "authorization_code"
          // "redirect" -> redirectUrl.value  ???
        ),
        Uri.unsafeFromString(tokenEndpoint.value)
      )

      token <- client.expect[TokenEndpointResponse](request)
    } yield token

  def fetchRefreshAccessToken(refreshToken: String): F[TokenEndpointResponse] =
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
    List("openid", "profile", "email", "offline_access").mkString(" ")
}
