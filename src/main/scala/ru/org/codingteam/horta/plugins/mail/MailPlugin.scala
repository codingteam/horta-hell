package ru.org.codingteam.horta.plugins.mail

import akka.actor.ActorRef
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import org.joda.time.DateTime
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.messages.LeaveReason
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

private object SendMailCommand

/**
 * Plugin for delivering the mail.
 */
class MailPlugin extends BasePlugin
  with CommandProcessor
  with ParticipantProcessor
  with DataAccessingPlugin[MailRepository] {

  import context.dispatcher

  implicit val timeout = Timeout(60.seconds)

  private val maxMessageCount = 5

  override def name = "mail"

  override def commands = List(CommandDefinition(CommonAccess, "send", SendMailCommand))

  override protected val schema = "mail"
  override protected val createRepository = MailRepository.apply _

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case SendMailCommand =>
        arguments match {
          case Array(receiver, message) if receiver.nonEmpty => sendMail(credential, receiver, message)
          case _ => Protocol.sendResponse(
            credential.location,
            credential,
            Localization.localize("Invalid arguments.")(credential))
        }
      case _ =>
    }
  }

  override def processParticipantJoin(time: DateTime, roomJID: String, participantJID: String, actor: ActorRef) {
    val participantNick = StringUtils.parseResource(participantJID)
    Credential.forNick(actor, participantNick) onSuccess { case receiver =>
      for (messages <- readMessages(roomJID, participantNick);
           message <- messages) {
        Protocol.sendPrivateResponse(actor, receiver, prepareText(message)(receiver)) map {
          case true => deleteMessage(message.id.get)
          case false =>
        }
      }
    }
  }

  override def processParticipantLeave(time: DateTime,
                                       roomJID: String,
                                       participantJID: String,
                                       reason: LeaveReason,
                                       actor: ActorRef) {}

  private def sendMail(sender: Credential, receiverNick: String, message: String) {
    implicit val c = sender
    import ru.org.codingteam.horta.localization.Localization._

    // First try to send the message right now:
    val location = sender.location
    val senderNick = sender.name
    Credential.forNick(location, receiverNick) onSuccess { case receiver =>
      Protocol.sendPrivateResponse(location, receiver, prepareText(senderNick, message)) map {
        case true =>
          Protocol.sendResponse(location, sender, localize("Message delivered."))

        case false =>
          val room = sender.roomId.get

          readMessages(room, receiverNick) map { messages =>
            val count = messages.length
            if (count > maxMessageCount) {
              Protocol.sendResponse(location, sender, localize("Message queue overflow."))
            } else {
              saveMessage(room, senderNick, receiverNick, message) map {
                case true => Protocol.sendResponse(location, sender, localize("Message enqueued."))
                case false => Protocol.sendResponse(location, sender, localize("Error while processing the message."))
              }
            }
          }
      }
    }
  }

  private def prepareText(message: MailMessage)(implicit credential: Credential): String =
    prepareText(message.senderNick, message.text)

  private def prepareText(senderNick: String, text: String)(implicit credential: Credential): String = {
    val template = Localization.localize("Message from %s")
    s"${template.format(senderNick)}: $text"
  }

  private def saveMessage(room: String, senderNick: String, receiverNick: String, message: String): Future[Boolean] = {
    val promise = Promise[Boolean]()
    withDatabase(_.store(MailMessage(None, room, senderNick, receiverNick, message))) onComplete { result =>
      promise.success(result.isSuccess)
    }

    promise.future
  }

  private def readMessages(room: String, receiverNick: String): Future[Seq[MailMessage]] = {
    withDatabase(_.getMessages(room, receiverNick))
  }

  private def deleteMessage(id: Int): Unit = {
    withDatabase(_.deleteMessage(id))
  }

}
