package db

import scala.concurrent.ExecutionContext

import cats.effect.kernel.{Async, Resource}

import doobie._
import doobie.hikari.HikariTransactor
import doobie.hikari.HikariTransactor.newHikariTransactor
import doobie.implicits._
import doobie.util.transactor
import doobie.util.transactor.Transactor.Aux

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
