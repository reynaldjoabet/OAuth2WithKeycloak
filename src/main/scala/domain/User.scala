package domain

import java.time.LocalDateTime

final case class User(
    userId: String,
    createdAt: LocalDateTime
)
