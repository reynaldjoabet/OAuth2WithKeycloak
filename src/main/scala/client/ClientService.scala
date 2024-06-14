package client

import java.util.UUID

import scala.concurrent.duration._

import cats.effect.kernel.Async
import cats.effect.std.UUIDGen
import cats.effect.syntax.all._
import cats.syntax.all._

import config._
import config.AllowedPostLogoutRedirectUrl
import domain.TokenEndpointResponse
import domain.UserInfoResponse
import domain.UserSession
import io.circe.syntax._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.RequestLogger
import org.http4s.client.middleware.ResponseLogger
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.Status
import retry.RetryPolicy
import services.TokenService

final case class ClientService[F[_]: Async](
  client: Client[F],
  tokenService: TokenService[F],
  uuidGen: UUIDGen[F]
) extends Http4sClientDsl[F] {

// Store State -> Redirect URL in Redis.
  // On callback, Redirecturl will be retrieved from Redis.
  // use authorization code flow with PKCE(recommended)
  // use a response_mode of query since this plays nicer with SameSite cookies(recommended)
  def makeKeycloakRedirect(frontendUrl: String): F[Uri] = for {
    credentials           <- ClientCredentials.credentials.load[F]
    authorizationEndpoint <- AuthorizationEndpoint.authorizationEndpoint.load[F]
    redirectUrl           <- RedirectUrl.redirectUrl.load[F]
    // state <- Async[F].delay(UUID.randomUUID().toString())
    state <- uuidGen.randomUUID.map(_.toString())
    _     <- tokenService.setState(state, frontendUrl, 40.seconds)
    queryParams = Map(
                    "response_type" -> "code",
                    "client_id"     -> credentials.clientId.value,
                    "redirect_uri"  -> redirectUrl.value,
                    "scope"         -> scopes,
                    "state"         -> state
                    // "response_mode"->"query"
                  )
  } yield Uri.unsafeFromString(authorizationEndpoint.value).withQueryParams(queryParams)

  def endUserSessionOnKeycloak(userSession: UserSession) = for {
    credentials           <- ClientCredentials.credentials.load[F]
    endSessionEndpoint    <- EndSessionEndpoint.endSessionEndpoint.load[F]
    postLogoutRedirectUrl <- AllowedPostLogoutRedirectUrl.postLogoutRedirectUrl.load[F]
    request = Method.POST(
                UrlForm(
                  "refresh_token" -> userSession.refreshToken,
                  "client_secret" -> credentials.clientSecret.value,
                  "client_id"     -> credentials.clientId.value
                  // "post_logout_redirect_uri" -> postLogoutRedirectUrl.value // Allowed Logout URLs
                ),
                Uri.unsafeFromString(endSessionEndpoint.value)
                // Authorization(
                // BasicCredentials(
                // credentials.clientId.value,
                // credentials.clientSecret.value
                //  )
                // )
                /// Authorization(
                // Credentials.Token(AuthScheme.Bearer, userSession.accessToken)
                // )
              )
    queryParams = Map(
                    "id_token_hint" -> userSession.idToken,
                    "redirect_url"  -> postLogoutRedirectUrl.value
                  )
    // _ <- ResponseLogger(true, true)(client).expect[String](request)
  } yield Uri.unsafeFromString(endSessionEndpoint.value).withQueryParams(queryParams)

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
      credentials   <- ClientCredentials.credentials.load[F]
      tokenEndpoint <- TokenEndpoint.tokenEndpoint.load[F]
      redirectUrl   <- RedirectUrl.redirectUrl.load[F]
      request = Method.POST(
                  UrlForm(
                    "client_id"     -> credentials.clientId.value,
                    "client_secret" -> credentials.clientSecret.value,
                    "code"          -> authorizationCode,
                    "grant_type"    -> "authorization_code",
                    "redirect_uri"  -> redirectUrl.value
                  ),
                  Uri.unsafeFromString(tokenEndpoint.value)
                )

      token <- client.expect[TokenEndpointResponse](request)
    } yield token

  def fetchNewAccessToken(refreshToken: String): F[TokenEndpointResponse] =
    for {
      tokenEndpoint <- TokenEndpoint.tokenEndpoint.load[F]
      credentials   <- ClientCredentials.credentials.load[F]
      request = Method.POST(
                  UrlForm(
                    "client_id"     -> credentials.clientId.value,
                    "client_secret" -> credentials.clientSecret.value,
                    "refresh_token" -> refreshToken,
                    "grant_type"    -> "refresh_token"
                  ),
                  Uri.unsafeFromString(tokenEndpoint.value)
                )

      token <- client.expect[TokenEndpointResponse](request)
    } yield token

//scopes enable a mechanism to define what an application can do on behalf of the user
  private val scopes =
    List("openid", "address").mkString(" ")

}

object ClientService {

  def make[F[_]: Async](
    client: Client[F],
    tokenService: TokenService[F],
    uuidGen: UUIDGen[F]
  ): ClientService[F] = ClientService(client, tokenService, uuidGen)

}
