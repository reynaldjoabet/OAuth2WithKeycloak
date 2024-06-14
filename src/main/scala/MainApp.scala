import java.io.InputStream
import java.net.NetworkInterface
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext.fromExecutorService
import scala.concurrent.ExecutionContext.global
import scala.jdk.javaapi.CollectionConverters._

import cats.effect
import cats.effect._
import cats.effect.kernel.syntax.resource
import cats.effect.std.syntax.supervisor
import cats.effect.std.Random
import cats.effect.std.Supervisor
import cats.effect.std.UUIDGen
import cats.effect.ExitCode
//import cats.syntax.semigroupk._
import cats.implicits._
import cats.Functor
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.net.tls.TLSContext
import fs2.io.net.tls.TLSParameters
import fs2.io.toInputStream

import client.ClientService
import com.comcast.ip4s._
import dev.profunktor.redis4cats.algebra
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data
import dev.profunktor.redis4cats.data.RedisCodec.Utf8
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.Redis
import domain.UserSession
import io.circe
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import kamon.http4s.middleware.server.KamonSupport
import kamon.instrumentation.http.HttpServerMetrics
import kamon.jaeger
import kamon.jaeger.JaegerReporter
import kamon.prometheus.PrometheusReporter
import kamon.zipkin.ZipkinReporter
import kamon.Kamon
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Referer
import org.http4s.server.defaults.Banner
import org.http4s.server.middleware
import org.http4s.server.middleware.CORS
import org.http4s.server.middleware.CSRF
import org.http4s.server.middleware.HSTS
import org.http4s.server.middleware.Metrics
import org.http4s.server.middleware.RequestLogger
import org.http4s.server.middleware.ResponseLogger
import org.http4s.server.AuthMiddleware
import org.http4s.server.Server
import org.http4s.Method._
import org.http4s.Uri
import org.typelevel.ci._
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.LoggerName
import routes._
import services._
import services.TokenService
import services.UserSessionService

object MainApp extends IOApp {

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
  val cookieName = "csrf-token"   // default

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
    .withGeneratedKey[IO, IO](request =>
      CSRF.defaultOriginCheck(request, "localhost", Uri.Scheme.http, None)
    )
    .map(builder =>
      builder
        // .withCookieName(cookieName)
        .withCookieDomain(Some("localhost"))
        .withCookiePath(Some("/"))
        .withCookieSecure(true)   // defaults to false
        .withCookieHttpOnly(true) // The CSRF token cookie must not have httpOnly flag,
        // defaults to true
        .withCookieName("__HOST-CSRF-TOKEN") // sent only to this host, no subdomains
        .build.validate()
    )
    .toResource

  override def run(args: List[String]): IO[ExitCode] =
    createSSLContext(password)
      .evalMap { sslContext =>
        RedisClient[IO]
          .from("redis://localhost")
          .use { redisClient =>
            (for {

              client <- EmberClientBuilder.default[IO].build
              redisCommands <- Redis[IO]
                                 .fromClient[String, UserSession](
                                   redisClient,
                                   UserSession.userSessionCodec
                                 )
                                 .evalOn(ec)

              redisCommandsUtf8 <- Redis[IO]
                                     .fromClient(
                                       redisClient,
                                       data.RedisCodec.Utf8
                                     )
                                     .evalOn(ec)

              context = TLSContext.Builder.forAsync[IO].fromSSLContext(sslContext)

              csrfMiddleware <- csrfService

              uuidGen      = UUIDGen[IO]
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
              // metrics <-
              // Resource.eval(
              // IO(HttpServerMetrics.of("http4s.server", "/127.0.0.1", 8097))
              // ).start
              allRoutes <- routes.allRoutes
              _ <- EmberServerBuilder
                     .default[IO]
                     .withHttpApp(
                       ResponseLogger.httpApp(true, true, _ => false)(
                         RequestLogger.httpApp(true, true, _ => false)(
                           allRoutes.orNotFound
                         )
                       )
                     )
                     .withPort(port"8097")
                     .withHost(host"0.0.0.0")

                     // .withTLS(context, tlsParameters)
                     // .withTLS(context)
                     // .withTLS(ssl)
                     // .withHttp2
                     // .withErrorHandler()//
                     .build.evalTap(showEmberBanner[IO](_))

            } yield ()).useForever
          }
      }
      .compile
      .drain
      .as(ExitCode.Success)

  private def createSSLContext(password: String): fs2.Stream[IO, SSLContext] =
    Files[IO]
      .readAll(Path("src/main/resources/localhost.keystore.p12"))
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
      getClass.getClassLoader.getResourceAsStream("localhost.keystore.p12")
    )
    val stream = Resource.make(load)(s => IO.blocking(s.close))
    stream.map(getSSLContext)
  }.flatMap(_.toResource)

  private def getSSLContext(in: InputStream): IO[TLSContext[IO]] = {
    val keyStore = KeyStore.getInstance("pkcs12")
    keyStore.load(in, password.toCharArray())
    // (keyStore, password.toCharArray())
    TLSContext.Builder.forAsync[IO].fromKeyStore(keyStore, password.toCharArray())
  }

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

  // server {
  //       host: "0.0.0.0" listening on all interface the server ,127.0.0.1,192.168.2.3 (for network). Not great for security
  //       host: ${?HOST}
  //       port: 7070
  //       port: ${?PORT}
  //     }
  // nterfaces are assigned unique identifiers, such as MAC (Media Access Control) addresses or IP (Internet Protocol) addresses.

  // Interfaces play a role in implementing security measures within a network. For example, firewalls, access control lists, and security policies are often configured at the interface level to control and filter traffic
  // Every Network Interface Card (NIC) is one device and has its own MAC address
}
