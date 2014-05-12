package ru.org.codingteam.horta.plugins.mail

import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, ParticipantProcessor, BasePlugin}
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.protocol.Protocol
import akka.actor.ActorRef
import org.jivesoftware.smack.util.StringUtils
import scala.concurrent.Future

private object SendMailCommand

/**
 * Plugin for delivering the mail.
 */
class MailPlugin extends BasePlugin with CommandProcessor with ParticipantProcessor {

  import context.dispatcher

  override def name = "mail"

  override def commands = List(CommandDefinition(CommonAccess, "send", SendMailCommand))

  // TODO: Implement DAO.

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case SendMailCommand =>
        arguments match {
          case Array(receiver, message) => sendMail(credential, receiver, message)
          case _ => Protocol.sendResponse(credential.location, credential, "Invalid arguments.")
        }
      case _ =>
    }
  }

  override def processParticipantJoin(roomJID: String, participantJID: String, actor: ActorRef) {
    val participantNick = StringUtils.parseResource(participantJID)
    val receiver = Credential.forNick(actor, participantNick)

    for (messages <- readMessages(roomJID, participantNick);
         message <- messages) {
      Protocol.sendPrivateResponse(actor, receiver, message.text) map {
        case true => deleteMessage(message.id)
        case false =>
      }
    }
  }

  override def processParticipantLeave(roomJID: String, participantJID: String, actor: ActorRef) {}

  private def sendMail(sender: Credential, receiverNick: String, message: String) {
    // First try to send the message right now:
    val location = sender.location
    val receiver = Credential.forNick(location, receiverNick)

    Protocol.sendPrivateResponse(location, receiver, message) map {
      case true =>
      case false => saveMessage(sender.roomName.get, receiverNick, message)
    }
  }

  private def saveMessage(room: String, receiverNick: String, message: String) {
    // TODO: Save the message.
  }

  private def readMessages(room: String, receiverNick: String): Future[Seq[MailMessage]] = {
    throw new Exception("Not implemented") // TODO: Read messages from the database.
  }

  private def deleteMessage(id: Int) {
    throw new Exception("Not implemented") // TODO: Delete message from the database.
  }

}
