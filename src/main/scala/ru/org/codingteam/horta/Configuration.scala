package ru.org.codingteam.horta

import java.util.Properties
import java.io.FileInputStream
import scala.collection.JavaConversions._

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

  lazy val login = properties.getProperty("login")
  lazy val password = properties.getProperty("password")
  lazy val server = properties.getProperty("server")
  lazy val nickname = properties.getProperty("nickname")

  lazy val rooms = {
    properties filterKeys (_.startsWith("room_"))
  }

  lazy val logDirectory = properties.getProperty("log_directory")
  lazy val logEncoding = properties.getProperty("log_encoding")
}
