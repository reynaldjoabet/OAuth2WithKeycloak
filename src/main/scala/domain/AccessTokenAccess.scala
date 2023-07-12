package domain
import io.circe.generic.semiauto.deriveCodec

final case class AccessTokenAccess(
    roles: Option[List[String]],
    verify_caller: Option[Boolean]
)

object AccessTokenAccess {
  implicit val codec = deriveCodec[AccessTokenAccess]
}
