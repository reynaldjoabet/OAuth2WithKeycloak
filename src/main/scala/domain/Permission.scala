package domain

import io.circe.generic.semiauto.deriveCodec

final case class Permission(
  claims: Option[Map[String, String]],
  rsid: Option[String],
  rsname: Option[String],
  scopes: Option[List[String]]
)

object Permission {
  implicit val codec = deriveCodec[Permission]

}
