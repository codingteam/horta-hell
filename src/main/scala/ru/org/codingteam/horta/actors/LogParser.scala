package ru.org.codingteam.horta.actors

import akka.actor.{ActorLogging, Actor}
import ru.org.codingteam.horta.messages.{ParsedPhrase, DoParsing}
import ru.org.codingteam.horta.Configuration
import java.io.File
import scalax.file.Path
import java.util.Scanner


class LogParser extends Actor with ActorLogging {
  val regex = "^\\[.*?\\] \\* (.*?)(?: \\*|:) (.*?)$".r

  def receive = {
    case DoParsing(roomName) => {
      val directory = Path.fromString(Configuration.logDirectory) / roomName
      log.info(s"Reading directoty $directory")

      for (path <- directory.descendants(depth = 1)) {
        val filename = path.path
        log.info(s"Reading file $filename")

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
    }
  }
}
