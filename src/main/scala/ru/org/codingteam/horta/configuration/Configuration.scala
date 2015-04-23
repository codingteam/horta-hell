package ru.org.codingteam.horta.configuration

import java.io.{StringReader, Reader, FileInputStream, InputStreamReader}
import java.nio.file.Path
import java.util.Properties

import ru.org.codingteam.horta.localization.LocaleDefinition

object Configuration {

  def initialize(configPath: Path): Unit = {
    config = Some(Left(configPath))
  }

  def initialize(content: String): Unit = {
    config = Some(Right(content))
  }

  private def openReader(): Reader = config match {
    case Some(Left(path)) => new InputStreamReader(new FileInputStream(path.toFile), "UTF8")
    case Some(Right(content)) => new StringReader(content)
    case None => sys.error("Configuration not defined")
  }

  private var config: Option[Either[Path, String]] = None

  private lazy val properties = {
    val properties = new Properties()
    val reader = openReader()
    try {
      properties.load(reader)
    } finally {
      reader.close()
    }

    properties
  }

  lazy val owner = properties.getProperty("owner")
  lazy val login = properties.getProperty("login")
  lazy val password = properties.getProperty("password")
  lazy val server = properties.getProperty("server")

  lazy val dftName = properties.getProperty("nickname")
  lazy val dftMessage = properties.getProperty("message")

  lazy val roomIds = Option(properties.getProperty("rooms")).map(_.split(",")).getOrElse(Array())
  lazy val roomDescriptors = roomIds map {
    case rid => new RoomDescriptor(
      rid,
      properties.getProperty(rid + ".room"),
      LocaleDefinition(properties.getProperty(rid + ".locale", defaultLocalization.name)),
      properties.getProperty(rid + ".nickname", dftName),
      properties.getProperty(rid + ".message", dftMessage))
  }

  lazy val markovMessagesPerMinute = properties.getProperty("markov_messages_per_minute", "5").toInt

  lazy val logDirectory = properties.getProperty("log_directory")
  lazy val logEncoding = properties.getProperty("log_encoding")

  lazy val defaultLocalization = LocaleDefinition(properties.getProperty("localization.default", "en"))

  lazy val storageUrl = properties.getProperty("storage.url")
  lazy val storageUser = properties.getProperty("storage.user")
  lazy val storagePassword = properties.getProperty("storage.password")
}
