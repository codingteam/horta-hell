package ru.org.codingteam.horta.configuration

import java.io.{FileInputStream, InputStreamReader, Reader, StringReader}
import java.nio.file.Path
import java.util.Properties

import ru.org.codingteam.horta.localization.LocaleDefinition

import scala.concurrent.duration._
import scala.language.postfixOps

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
        .getOrElse(sys.error(s"Configuration not found at '$configPath'.")))
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
  lazy val xmppTimeout: FiniteDuration =
    Option(properties.getProperty("xmpp.timeout_ms"))
      .map(Integer.parseInt)
      .getOrElse(5000).milliseconds

  lazy val dftName = properties.getProperty("nickname")
  lazy val dftMessage = properties.getProperty("message")

  lazy val roomIds = Option(properties.getProperty("rooms")).map(_.split(",")).getOrElse(Array())
  lazy val roomDescriptors = roomIds.map({
    case rid => (rid, RoomDescriptor(
      rid,
      properties.getProperty(rid + ".room"),
      LocaleDefinition(properties.getProperty(rid + ".locale", defaultLocalization.name)),
      properties.getProperty(rid + ".nickname", dftName),
      properties.getProperty(rid + ".message", dftMessage),
      properties.getProperty(rid + ".events", "")))
  }).toMap

  lazy val markovMessagesPerMinute = properties.getProperty("markov_messages_per_minute", "5").toInt
  lazy val markovMessageWordLimit = properties.getProperty("markov_message_word_limit", "30").toInt

  lazy val defaultLocalization = LocaleDefinition(properties.getProperty("localization.default", "en"))

  lazy val storageUrl = properties.getProperty("storage.url")
  lazy val storageUser = properties.getProperty("storage.user")
  lazy val storagePassword = properties.getProperty("storage.password")

  lazy val loglistUrl = properties.getProperty("loglist.url", "https://loglist.net")

  lazy val petRoomNames = properties.getProperty("pet.rooms", "").split(',').map(_.trim)
  lazy val petRoomIds = petRoomNames.map(roomDescriptors).map(_.room).toSet

  def apply(key: String): String = {
    properties.getProperty(key)
  }
}
