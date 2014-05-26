package ru.org.codingteam.horta.plugins.log

import ru.org.codingteam.horta.plugins.{MessageProcessor, ParticipantProcessor, BasePlugin}
import akka.actor.ActorRef
import ru.org.codingteam.horta.security.Credential
import org.joda.time.DateTime

class LogPlugin extends BasePlugin with ParticipantProcessor with MessageProcessor {

  override protected def name: String = "logger"

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
