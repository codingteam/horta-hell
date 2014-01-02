package ru.org.codingteam.horta.configuration

import java.util.Properties
import java.io.FileInputStream

object Configuration {
	private lazy val properties = {
		val properties = new Properties()
		val stream = new FileInputStream("horta.properties")
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
      properties.getProperty(rid+".room"),
      properties.getProperty(rid+".nickname", dftName),
      properties.getProperty(rid+".message", dftMessage))
  }

	lazy val logDirectory = properties.getProperty("log_directory")
	lazy val logEncoding = properties.getProperty("log_encoding")
}
