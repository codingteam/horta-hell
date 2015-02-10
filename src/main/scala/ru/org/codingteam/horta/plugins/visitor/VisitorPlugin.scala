package ru.org.codingteam.horta.plugins.visitor

import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils

import ru.org.codingteam.horta.messages.GetParticipants
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.protocol.jabber.Visitor
import ru.org.codingteam.horta.security.{RoomAdminAccess, Credential}

import scala.concurrent.duration._
import scala.language.postfixOps

object VisitorsCommand

class VisitorPlugin extends CommandProcessor {

  import context.dispatcher
  implicit val timeout = Timeout(60 seconds)

  /**
   * Plugin name.
   * @return unique plugin name.
   */
  override protected def name: String = "visitor"

  /**
   * A collection of command definitions.
   * @return a collection.
   */
  override protected def commands: List[CommandDefinition] =
    List(CommandDefinition(RoomAdminAccess, "visitors", VisitorsCommand))

  /**
   * Process a command.
   * @param credential a credential of a user executing the command.
   * @param token token registered for command.
   * @param arguments command argument array.
   */
  override protected def processCommand(credential: Credential, token: Any, arguments: Array[String]): Unit = {
    (credential.location ? GetParticipants).mapTo[Protocol.ParticipantCollection] map { case participants =>
      val visitors = participants.values.filter(_.role == Visitor).map {
        participant => StringUtils.parseResource(participant.jid)
      }

      Protocol.sendResponse(credential.location, credential, visitors.mkString(", "))
    }
  }

}
