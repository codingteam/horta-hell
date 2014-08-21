package ru.org.codingteam.horta.plugins.mail

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import org.joda.time.DateTime
import ru.org.codingteam.horta.database.{DeleteObject, StoreObject, ReadObject}
import ru.org.codingteam.horta.messages.LeaveReason
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, ParticipantProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.concurrent.duration._
import scala.concurrent.Future

private object SendMailCommand

/**
 * Plugin for delivering the mail.
 */
class MailPlugin extends BasePlugin with CommandProcessor with ParticipantProcessor {

  import context.dispatcher

  implicit val timeout = Timeout(60.seconds)

  private val maxMessageCount = 5

  override def name = "mail"

  override def commands = List(CommandDefinition(CommonAccess, "send", SendMailCommand))

  override def dao = Some(new MailDAO())

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case SendMailCommand =>
        arguments match {
          case Array(receiver, message) if receiver.nonEmpty => sendMail(credential, receiver, message)
          case _ => Protocol.sendResponse(credential.location, credential, "Invalid arguments.")
        }
      case _ =>
    }
  }

  override def processParticipantJoin(time: DateTime, roomJID: String, participantJID: String, actor: ActorRef) {
    val participantNick = StringUtils.parseResource(participantJID)
    val receiver = Credential.forNick(actor, participantNick)

    for (messages <- readMessages(roomJID, participantNick);
         message <- messages) {
      Protocol.sendPrivateResponse(actor, receiver, prepareText(message)) map {
        case true => deleteMessage(message.id.get)
        case false =>
      }
    }
  }

  override def processParticipantLeave(time: DateTime,
                                       roomJID: String,
                                       participantJID: String,
                                       reason: LeaveReason,
                                       actor: ActorRef) {}

  private def sendMail(sender: Credential, receiverNick: String, message: String) {
    // First try to send the message right now:
    val location = sender.location
    val senderNick = sender.name
    val receiver = Credential.forNick(location, receiverNick)

    Protocol.sendPrivateResponse(location, receiver, prepareText(senderNick, message)) map {
      case true =>
        Protocol.sendResponse(location, sender, "Сообщение доставлено")

      case false =>
        val room = sender.roomId.get

        readMessages(room, receiverNick) map { messages =>
          val count = messages.length
          if (count > maxMessageCount) {
            Protocol.sendResponse(location, sender, "Очередь сообщений указанного пользователя переполнена")
          } else {
            saveMessage(room, senderNick, receiverNick, message) map {
              case true => Protocol.sendResponse(location, sender, "Сообщение помещено в очередь")
              case false => Protocol.sendResponse(location, sender, "Ошибка при обработке сообщения")
            }
          }
        }
    }
  }

  private def prepareText(message: MailMessage): String = prepareText(message.senderNick, message.text)

  private def prepareText(senderNick: String, text: String): String = {
    s"Сообщение от $senderNick: $text"
  }

  private def saveMessage(room: String, senderNick: String, receiverNick: String, message: String): Future[Boolean] = {
    (store ? StoreObject(name, None, MailMessage(None, room, senderNick, receiverNick, message))).map {
      case Some(_) => true
      case None => false
    }
  }

  private def readMessages(room: String, receiverNick: String): Future[Seq[MailMessage]] = {
    (store ? ReadObject(name, (room, receiverNick))) map {
      case Some(messages: Seq[MailMessage]) => messages
    }
  }

  private def deleteMessage(id: Int) {
    store ? DeleteObject(name, id)
  }

}
