package config
import org.postgresql
import ciris.ConfigDecoder

final case class DataSourceConfiguration private (
    databaseName: String,
    user: String,
    password: String,
    portNumber: Int,
    serverName: String
)

object DataSourceConfiguration {
  implicit val configDecoder = ConfigDecoder[String]
}
