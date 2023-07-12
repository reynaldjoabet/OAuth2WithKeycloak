import cats.effect.ExitCode
import cats.effect._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._
import org.http4s.server.defaults.Banner
import org.http4s.server.Server
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.LoggerName
import io.circe
import routes._
import services._
import org.http4s.server.middleware.RequestLogger
import cats.effect.std.Random

object Main extends IOApp {
//implicit val loggerName=LoggerName("name")
  private implicit val logger = Slf4jLogger.getLogger[IO]

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"
    )

  override def run(args: List[String]): IO[ExitCode] =
    Random
      .scalaUtilRandom[IO]
      .flatMap { random =>
        EmberServerBuilder
          .default[IO]
          // .withHttpApp()
          .withPort(port"8085")
          .withHost(host"127.0.0.1")
          .build
          .evalTap(showEmberBanner[IO](_))
          .useForever
      }
      .as(ExitCode.Success)

}
