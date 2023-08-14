package domain
import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
import io.circe.Encoder
import io.circe.Decoder

final case class AccessTokenAccess(
    roles: Option[List[String]],
    verifyCaller: Option[Boolean]
)

object AccessTokenAccess {
  implicit val codec = deriveCodec[AccessTokenAccess]
  val decoder = Decoder.instance { h =>
    for {
      roles <- h.get[Option[List[String]]]("roles")
      verifyCaller <- h.get[Option[Boolean]]("verify_caller")
    } yield AccessTokenAccess(roles, verifyCaller)
  }
  implicit val codec1 = Codec.from(???, ???)
}
