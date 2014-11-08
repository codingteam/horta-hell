package ru.org.codingteam.horta.plugins.markov

import java.io.File
import java.util.Scanner

import akka.event.LoggingAdapter
import me.fornever.platonus.Network
import ru.org.codingteam.horta.configuration.Configuration

object LogParser {

  val regex = "^\\[.*?\\] \\* (.*?)(?: \\*|:) (.*?)$".r

  def parse(log: LoggingAdapter, roomName: String, userName: String) = {
    val directory = new File(new File(Configuration.logDirectory), roomName)
    val network = Network(2)

    log.info(s"Reading directory $directory")
    try {
      for (file <- directory.listFiles()) {
        val scanner = new Scanner(file, Configuration.logEncoding).useDelimiter("\\r\\n")
        try {
          while (scanner.hasNext) {
            val string = scanner.next()
            string match {
              case regex(nick, message) if nick == userName =>
                network.add(LogParser.tokenize(message))
              case _ =>
            }
          }
        } finally {
          scanner.close()
        }
      }
    } catch {
      case e: Throwable =>
        log.error(e, s"Error reading $directory")
    }

    log.info(s"Finished reading $directory")
    network
  }

  def tokenize(message: String) = {
    message.split("\\s+").toVector
  }

}