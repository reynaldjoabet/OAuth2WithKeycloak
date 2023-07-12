package domain
import java.time.Instant

/** @param sessionId
  *   the user's session cookie
  * @param userId
  *   the user's keycloak id
  */
final case class UserSession(
    sessionId: String,
    userId: String,
    roles: Set[String],
    accessToken: String,
    refreshToken: String,
    expiration: Int
)
object UserSession {}
