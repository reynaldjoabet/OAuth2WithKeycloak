package domain
import io.circe.generic.semiauto.deriveCodec

final case class AccessTokenAuthorization(
    permissions: Option[List[Permission]]
)

object AccessTokenAuthorization {
  implicit val codec = deriveCodec[AccessTokenAuthorization]

}
