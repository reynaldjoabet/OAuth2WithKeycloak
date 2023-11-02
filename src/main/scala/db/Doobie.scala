package db

import doobie._
import doobie.implicits._
import doobie.util.transactor
import cats.effect.kernel.{Async, Resource}
import doobie.hikari.HikariTransactor
import doobie.hikari.HikariTransactor.newHikariTransactor
import doobie.util.transactor.Transactor.Aux

import scala.concurrent.ExecutionContext

object Doobie {

  private val ec = ExecutionContext.global

//HikariTransactor
  def hikariTransactor[F[_]: Async]: Resource[F, HikariTransactor[F]] = newHikariTransactor[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:social_db",
    "postgres",
    "",
    ec
  )

}
