package domain
import io.circe.generic.semiauto.deriveCodec

final case class Address(
    country: Option[String],
    formatted: Option[String],
    locality: Option[String],
    postal_code: Option[String],
    region: Option[String],
    street_address: Option[String]
)

object Address {
  implicit val codec = deriveCodec[Address]

}
