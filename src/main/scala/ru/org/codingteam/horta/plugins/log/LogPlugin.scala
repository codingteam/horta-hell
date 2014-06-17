package ru.org.codingteam.horta.plugins.log

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import org.joda.time.DateTime
import ru.org.codingteam.horta.database.{ReadObject, DAO, StoreObject}
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.Future
import scala.concurrent.duration._

object SearchLogCommand

class LogPlugin extends BasePlugin with ParticipantProcessor with MessageProcessor with CommandProcessor {

  implicit val timeout = Timeout(60.seconds)

  import context.dispatcher

  override protected def name: String = "log"

  override protected def dao: Option[DAO] = Some(new LogDAO())

  override protected def commands: List[CommandDefinition] = List(
    CommandDefinition(CommonAccess, "search", SearchLogCommand))

  override protected def processParticipantLeave(time: DateTime,
                                                 roomJID: String,
                                                 participantJID: String,
                                                 actor: ActorRef) {
    saveLogMessage(time, roomJID, participantJID, EnterType)
  }

  override protected def processParticipantJoin(time: DateTime,
                                                roomJID: String,
                                                participantJID: String,
                                                actor: ActorRef) {
    saveLogMessage(time, roomJID, participantJID, LeaveType)
  }

  override protected def processMessage(time: DateTime, credential: Credential, message: String) {
    saveLogMessage(time, credential.roomId.get, credential.name, MessageType, message)
  }

  def processCommand(credential: Credential,
                     token: Any,
                     arguments: Array[String]) = {
    val location = credential.location
    (token, arguments) match {
      case (SearchLogCommand, Array(phrase)) =>
        for (response <- getSearchResponse(credential.roomId.get, phrase)) {
          Protocol.sendResponse(location, credential, response)
        }
      case _ => Protocol.sendResponse(location, credential, "Invalid command.")
    }
  }

  private def saveLogMessage(time: DateTime,
                             roomJID: String,
                             participantJID: String,
                             eventType: EventType) {
    val sender = StringUtils.parseResource(participantJID)
    saveLogMessage(time, roomJID, sender, EnterType, "")
  }

  private def saveLogMessage(time: DateTime,
                             roomJID: String,
                             sender: String,
                             eventType: EventType,
                             text: String) {
    val message = LogMessage(None, time, roomJID, sender, eventType, text)
    store ? StoreObject(name, None, message)
  }

  private def getSearchResponse(room:String, phrase: String): Future[String] = {
    (store ? ReadObject(name, GetMessages(room, phrase))) map {
      case Some(messages: Seq[LogMessage]) =>
        messages.map(message => s"${message.time} ${message.sender} ${message.text}").mkString("\n")
    }
  }

}
