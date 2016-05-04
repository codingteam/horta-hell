package ru.org.codingteam.horta.configuration

import java.io.{StringReader, Reader, FileInputStream, InputStreamReader}
import java.nio.file.Path
import java.util.Properties

import ru.org.codingteam.horta.localization.LocaleDefinition

object Configuration {

  def initialize(path: Path): Unit = {
    configPath = Some(path)
  }

  def initialize(content: String): Unit = {
    configContent = Some(content)
  }

  private def openReader(): Reader = {
    configContent.map(content => new StringReader(content))
      .getOrElse(configPath.map(path => new InputStreamReader(new FileInputStream(path.toFile), "UTF8"))
      .getOrElse(sys.error("Configuration not defined")))
  }

  private var configPath: Option[Path] = None
  private var configContent: Option[String] = None

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
  lazy val markovMessageWordLimit = properties.getProperty("markov_message_word_limit", "30").toInt

  lazy val defaultLocalization = LocaleDefinition(properties.getProperty("localization.default", "en"))

  lazy val storageUrl = properties.getProperty("storage.url")
  lazy val storageUser = properties.getProperty("storage.user")
  lazy val storagePassword = properties.getProperty("storage.password")
}
