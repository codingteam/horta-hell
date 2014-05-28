package ru.org.codingteam.horta.plugins.log

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import org.joda.time.DateTime
import ru.org.codingteam.horta.plugins.{BasePlugin, MessageProcessor, ParticipantProcessor}
import ru.org.codingteam.horta.database.{DAO, StoreObject}
import ru.org.codingteam.horta.security.Credential
import scala.concurrent.duration._

class LogPlugin extends BasePlugin with ParticipantProcessor with MessageProcessor {

  import context.dispatcher
  implicit val timeout = Timeout(60.seconds)

  override protected def name: String = "log"

  override protected def dao: Option[DAO] = Some(new LogDAO())

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
    val message = LogMessage(None, time, roomJID, sender, EnterType, text)
    store ? StoreObject(name, None, message)
  }

}
