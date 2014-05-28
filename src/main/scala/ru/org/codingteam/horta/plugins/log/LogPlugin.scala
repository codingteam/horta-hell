package ru.org.codingteam.horta.plugins.log

import akka.actor.ActorRef
import org.joda.time.DateTime
import ru.org.codingteam.horta.plugins.{BasePlugin, MessageProcessor, ParticipantProcessor}
import ru.org.codingteam.horta.database.DAO
import ru.org.codingteam.horta.security.Credential

class LogPlugin extends BasePlugin with ParticipantProcessor with MessageProcessor {

  override protected def name: String = "log"

  override protected def dao: Option[DAO] = Some(new LogDAO())

  override protected def processParticipantLeave(time: DateTime,
                                                 roomJID: String,
                                                 participantJID: String,
                                                 actor: ActorRef): Unit = ???

  override protected def processParticipantJoin(time: DateTime,
                                                roomJID: String,
                                                participantJID: String,
                                                actor: ActorRef): Unit = ???

  override protected def processMessage(time: DateTime, credential: Credential, message: String): Unit = ???

}
