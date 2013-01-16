package ru.org.codingteam.horta.actors

import akka.actor.{Actor, ActorLogging}
import java.io.File
import java.util.Scanner
import ru.org.codingteam.horta.Configuration
import ru.org.codingteam.horta.messages.{DoParsing, ParsedPhrase}
import scalax.file.Path

class LogParser extends Actor with ActorLogging {
  val regex = "^\\[.*?\\] \\* (.*?)(?: \\*|:) (.*?)$".r

  def receive = {
    case DoParsing(roomName) => {
      val directory = Path.fromString(Configuration.logDirectory) / roomName
      log.info(s"Reading directoty $directory")

      for (path <- directory.descendants(depth = 1)) {
        val filename = path.path

        val file = new File(filename)
        val scanner = new Scanner(file, Configuration.logEncoding).useDelimiter("\\r\\n")
        try {
          while (scanner.hasNext) {
            val string = scanner.next()
            string match {
              case regex(nick, message) => sender ! ParsedPhrase(nick, message)
              case _ =>
            }
          }
        } finally {
          scanner.close()
        }
      }

      log.info(s"Finished reading $directory")
    }
  }
}
