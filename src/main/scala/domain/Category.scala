package domain

import io.circe.Decoder
import io.circe.Encoder

sealed abstract class Category

object Category {

  case object INTERNAL               extends Category
  case object ACCESS                 extends Category
  case object ID                     extends Category
  case object ADMIN                  extends Category
  case object USERINFO               extends Category
  case object LOGOUT                 extends Category
  case object AUTHORIZATION_RESPONSE extends Category

  implicit val decoder: Decoder[Category] = Decoder[String].emap[Category] {
    case "INTERNAL"                => Right(INTERNAL)
    case "ACCESS"                  => Right(ACCESS)
    case "ID"                      => Right(ID)
    case "ADMIN"                   => Right(ADMIN)
    case "USERINFO"                => Right(USERINFO)
    case "LOGOUT "                 => Right(LOGOUT)
    case "AUTHORIZATION_RESPONSE " => Right(AUTHORIZATION_RESPONSE)

  }

  implicit val encoder = Encoder[String].contramap[Category] {
    case INTERNAL               => "INTERNAL"
    case ACCESS                 => "ACCESS"
    case ID                     => "ID"
    case ADMIN                  => "ADMIN"
    case USERINFO               => "USERINFO`"
    case LOGOUT                 => "LOGOUT"
    case AUTHORIZATION_RESPONSE => "AUTHORIZATION_RESPONSE"
  }

}
