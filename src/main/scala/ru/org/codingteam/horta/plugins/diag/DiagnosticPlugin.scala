package ru.org.codingteam.horta.plugins.diag

import akka.util.Timeout
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.protocol.xmpp.Xmpp
import ru.org.codingteam.horta.security.{Credential, RoomAdminAccess}

import scala.concurrent.duration._

case object DiagCommand

class DiagnosticPlugin extends CommandProcessor {

  import context.dispatcher
  implicit val timeout = Timeout(60.seconds)

  override protected def name: String = "diagnostic"

  /**
   * A collection of command definitions.
   * @return a collection.
   */
  override protected def commands: List[CommandDefinition] = List(
    CommandDefinition(RoomAdminAccess, "diag", DiagCommand))

  override protected def processCommand(credential: Credential, token: Any, arguments: Array[String]): Unit = {
    token match {
      case DiagCommand =>
        arguments match {
          case Array("participants") =>
            val location = credential.location
            Protocol.getParticipants(location) map { participants =>
              val nicknames = participants.toStream.map(item => Xmpp.nickByJid(item._1))
              val message = nicknames.mkString(", ")
              Protocol.sendResponse(location, credential, message)
            }
        }
    }
  }
}
