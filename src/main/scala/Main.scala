import services.TokenService
import services.UserSessionService
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
import org.http4s.ember.client.EmberClientBuilder
import client.ClientService
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.algebra
import dev.profunktor.redis4cats.data.RedisCodec.Utf8
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._
import domain.UserSession
import dev.profunktor.redis4cats.data
import org.http4s.server.AuthMiddleware
import cats.Functor
import cats.effect.std.UUIDGen
import cats.effect.std.Supervisor
import cats.effect.std.syntax.supervisor
import org.http4s.server.middleware.ResponseLogger
import org.http4s.server.middleware.CSRF
import org.http4s.Uri
import org.http4s.server.middleware.CORS
import org.typelevel.ci._
import org.http4s.Method._
//import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext.fromExecutorService
import java.util.concurrent.Executors
import org.http4s.headers.Referer
import fs2.io.net.tls.TLSContext
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import java.security.SecureRandom
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.toInputStream
import cats.effect.kernel.syntax.resource
import org.http4s.server.middleware.HSTS
import fs2.io.net.tls.TLSParameters
import scala.jdk.javaapi.CollectionConverters._
import javax.net.ssl.SSLContext

object Main extends IOApp {

  val password = "password" // .toCharArray()

  val ec = fromExecutorService(Executors.newWorkStealingPool(5))
//implicit val loggerName=LoggerName("name")
  // override protected def blockedThreadDetectionEnabled = true
  implicit private val logger = Slf4jLogger.getLogger[IO]

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}"
    )

  val headerName = "X-Csrf-Token" // default
  val cookieName = "csrf-token" // default

  private val corsService = CORS
    .policy
    .withAllowOriginHost(Set("http://localhost:3000"))
    .withAllowMethodsIn(Set(POST, PUT, GET, DELETE))
    .withAllowCredentials(
      false
    ) // set to true for csrf// The default behavior of cross-origin resource requests is for
    // requests to be passed without credentials like cookies and the Authorization header
    // Cookies are not set on cross-origin requests (CORS) by default. To enable cookies on an API, you will set Access-Control-Allow-Credentials=true.
    // The browser will reject any response that includes Access-Control-Allow-Origin=*
    .withAllowHeadersIn(Set(ci"X-Csrf-Token", ci"Content-Type"))

  def csrfService = CSRF
    .withGeneratedKey[IO, IO](request => CSRF.defaultOriginCheck(request, "localhost", Uri.Scheme.https, None))
    .map(builder =>
      builder
        // .withCookieName(cookieName)
        .withCookieDomain(Some("localhost"))
        .withCookiePath(Some("/"))
        .withCookieSecure(true) // defaults to false
        .withCookieHttpOnly(false) // defaults to true
        .withCookieName("__HOST-CSRF-TOKEN")
        .build
        .validate()
    )
    .toResource

  override def run(args: List[String]): IO[ExitCode] =
    createSSLContext(password).evalMap { sslContext =>
      RedisClient[IO]
        .from("redis://localhost")
        .use { redisClient =>
          (for {

            client <- EmberClientBuilder
                        .default[IO]
                        .build
            redisCommands <- Redis[IO]
                               .fromClient[String, UserSession](
                                 redisClient,
                                 UserSession.userSessionCodec
                               )

            redisCommandsUtf8 <- Redis[IO]
                                   .fromClient(
                                     redisClient,
                                     data.RedisCodec.Utf8
                                   )

            context = TLSContext
                        .Builder
                        .forAsync[IO]
                        .fromSSLContext(sslContext)

            csrfMiddleware <- csrfService

            uuidGen = UUIDGen[IO]
            tokenService = TokenService.make(redisCommandsUtf8)

            clientService = ClientService.make(client, tokenService, uuidGen)
            userSessionService = UserSessionService.make(
                                   redisCommands,
                                   clientService
                                 )

            routes = AuthenticationRoutes(
                       clientService,
                       userSessionService,
                       tokenService,
                       uuidGen
                     )
            _ <- EmberServerBuilder
                   .default[IO]
                   .withHttpApp(
                     csrfMiddleware(
                       ResponseLogger.httpApp(true, true, _ => false)(
                         RequestLogger.httpApp(true, true, _ => false)(
                           (routes.allRoutes).orNotFound
                         )
                       )
                     )
                   )
                   .withPort(port"8097")
                   .withHost(host"localhost")
                   .withTLS(context, tlsParameters)
                   // .withTLS(context)
                   // .withTLS(ssl)
                   // .withHttp2
                   // .withErrorHandler()//
                   .build
                   .evalTap(showEmberBanner[IO](_))

          } yield ()).useForever
        }
    }.compile
      .drain
      .as(ExitCode.Success)

  private def createSSLContext(password: String) =
    Files[IO]
      .readAll(Path("src/main/resources/serverkeystore.p12"))
      // .evalTap(IO.println)
      .through(toInputStream)
      .map { inputStream =>
        val keyStore =
          KeyStore.getInstance("PKCS12") // KeyStore.getDefaultType()

        keyStore.load(inputStream, password.toCharArray())
        val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        keyManagerFactory.init(keyStore, password.toCharArray())
        // SSLContext.getDefault().getDefaultSSLParameters().getProtocols().foreach(println)
        // SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites().foreach(
        // println
        /// )
        val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
        trustManagerFactory.init(keyStore)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
          keyManagerFactory.getKeyManagers(),
          null,
          // trustManagerFactory.getTrustManagers(),
          new SecureRandom
        )
        sslContext
      }

  // Each key store has an overall password used to protect the entire store, and can optionally have per-entry passwords for each secret- or private-key entry (if your backend supports it).
  private def keyStore(password: String) = {
    val load = IO.blocking(
      getClass.getClassLoader.getResourceAsStream("serverkeystore.p12")
    )
    val stream = Resource.make(load)(s => IO.blocking(s.close))
    stream.map { inputStream =>
      val keyStore = KeyStore.getInstance("pkcs12")
      keyStore.load(inputStream, password.toCharArray())
      (keyStore, password.toCharArray())
      TLSContext
        .Builder
        .forAsync[IO]
        .fromKeyStore(keyStore, password.toCharArray())
    }
  }.flatMap(_.toResource)

// in TLS 1.3, TLS_AES_128_CCM_8_SHA256 and TLS_AES_128_CCM_SHA256 are marked as CAN implement
// TLS_AES_128_GCM_SHA256 MUST implement
// TLS_AES_256_GCM_SHA384 and TLS_CHACHA20_POLY1305_SHA256 are marked as SHOULD implement
// jdk 17 implements the MUST and SHOULD
  lazy val tlsParameters = TLSParameters(protocols =
    Some(List("TLSv1.3", "TLSv1.2")) // default protocols are 1.2 and 1.3
  // cipherSuites = Some(List("TLS_AES_128_GCM_SHA256","TLS_AES_256_GCM_SHA384","TLS_CHACHA20_POLY1305_SHA256")))
  // List("TLS_AES_128_CCM_8_SHA256","TLS_AES_128_CCM_SHA256","TLS_AES_128_GCM_SHA256","TLS_AES_256_GCM_SHA384","TLS_CHACHA20_POLY1305_SHA256")
  // Unsupported CipherSuite: TLS_AES_128_CCM_8_SHA256
  // CipherSuite: TLS_AES_128_CCM_SHA256
  )

}
