package config

import ciris.ConfigDecoder
import org.postgresql

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
