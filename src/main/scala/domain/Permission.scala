package domain
import io.circe.generic.semiauto.deriveCodec
import io.circe.Decoder
final case class Permission(
    claims: Option[Map[String, String]],
    rsid: Option[String],
    rsname: Option[String],
    scopes: Option[List[String]]
)

object Permission {
  implicit val codec = deriveCodec[Permission]

}
