package db

import config.FlywayConfiguration
import org.flywaydb.core.Flyway
import cats.effect.IO
import cats.effect.std

object DBMigration {

  def migrate(): IO[Unit] =
    for {
      flywayConfig <- FlywayConfiguration.flywayConfig.load[IO]
      flyway <- IO.delay(
                  Flyway
                    .configure()
                    .dataSource(flywayConfig.url, flywayConfig.username, flywayConfig.password)
                    .load()
                )
      _ <- IO.delay(flyway.migrate())
    } yield ()

  def reset() =
    for {
      _ <- std.Console[IO].println("RESETTING DATABASE!")
      flywayConfig <- FlywayConfiguration.flywayConfig.load[IO]
      flyway <- IO.delay(
                  Flyway
                    .configure()
                    .dataSource(flywayConfig.url, flywayConfig.username, flywayConfig.password)
                    .cleanDisabled(false)
                    .load()
                )
      _ <- IO.delay(flyway.clean())
    } yield ()

}
