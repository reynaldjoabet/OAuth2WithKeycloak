package domain

import java.time.LocalDateTime

import cats.instances.AllInstances

import io.circe.Encoder

final case class User(
  userId: String,
  createdAt: LocalDateTime
)

object User {

  class UserName(val name: String) extends AnyVal

  val encoderUsername: Encoder[UserName] = Encoder[String].contramap[UserName](_.name)

}
