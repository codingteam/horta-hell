package ru.org.codingteam.horta.plugins.markov

import java.io.File
import java.util.Scanner
import scalax.file.Path
import ru.org.codingteam.horta.configuration.Configuration
import me.fornever.platonus.Network
import akka.event.LoggingAdapter

object LogParser {
  val regex = "^\\[.*?\\] \\* (.*?)(?: \\*|:) (.*?)$".r

  def parse(log: LoggingAdapter, roomName: String, userName: String) = {
    val directory = Path.fromString(Configuration.logDirectory) / roomName
    val network = Network(2)

    log.info(s"Reading directory $directory")
    try {
      for (path <- directory.descendants(depth = 1)) {
        val filename = path.path

        val file = new File(filename)
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