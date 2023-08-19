package domain
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax._
import io.circe.parser.decode
import dev.profunktor.redis4cats.codecs.Codecs
import dev.profunktor.redis4cats.codecs.splits.SplitEpi
import dev.profunktor.redis4cats.data._

/** @param sessionId
  *   the user's session cookie
  * @param userId
  *   the user's keycloak id
  */
final case class UserSession(
    sessionId: String,
    userId: String,
    roles: List[String],
    accessToken: String,
    refreshToken: String,
    expiration: Int,
    idToken: String
)
object UserSession {
  implicit val codec = deriveCodec[UserSession]

  val userSessionSplit = SplitEpi[String, UserSession](
    str => decode[UserSession](str).toOption.get,
    _.asJson.noSpaces
  )

  val userSessionCodec: RedisCodec[String, UserSession] =
    Codecs.derive(RedisCodec.Utf8, userSessionSplit)

}
