package ru.org.codingteam.horta.configuration

import java.util.Properties
import java.io.{FileInputStream, InputStreamReader}

object Configuration {
  private lazy val properties = {
    val properties = new Properties()
    val stream = new InputStreamReader(new FileInputStream("horta.properties"), "UTF8")
    try {
      properties.load(stream)
    } finally {
      stream.close()
    }
    properties
  }

  lazy val owner = properties.getProperty("owner")
  lazy val login = properties.getProperty("login")
  lazy val password = properties.getProperty("password")
  lazy val server = properties.getProperty("server")

  lazy val dftName = properties.getProperty("nickname")
  lazy val dftMessage = properties.getProperty("message")

  lazy val roomIds = properties.getProperty("rooms").split(",")
  lazy val roomDescriptors = roomIds map {
    case rid => new RoomDescriptor(
      rid,
      properties.getProperty(rid + ".room"),
      properties.getProperty(rid + ".nickname", dftName),
      properties.getProperty(rid + ".message", dftMessage))
  }

  lazy val markovMessagesPerMinute = properties.getProperty("markov_messages_per_minute", "5").toInt

  lazy val logDirectory = properties.getProperty("log_directory")
  lazy val logEncoding = properties.getProperty("log_encoding")

  lazy val storageUrl = properties.getProperty("storage.url")
  lazy val storageUser = properties.getProperty("storage.user")
  lazy val storagePassword = properties.getProperty("storage.password")
}
