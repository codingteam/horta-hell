package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.{SendChatMessage, SendResponse}
import ru.org.codingteam.horta.security.{CommonAccess, Credential, GlobalAccess}
import ru.org.codingteam.horta.configuration.Configuration
import scala.concurrent.duration._
import scala.Some

class PrivateMessageHandler(val protocol: ActorRef) extends Actor with ActorLogging {

  val core = context.actorSelection("/user/core")

  override def receive = {
    case UserMessage(message) =>
      val jid = message.getFrom
      val text = message.getBody

      log.info(s"Private message: <$jid> $text")
      if (text != null) {
        val credential = getCredential(jid)
        core ! CoreMessage(Clock.now, credential, text)
      }

    case SendResponse(credential, text) =>
      val jid = credential.id.get.asInstanceOf[String]

      implicit val timeout = Timeout(60.seconds)
      import context.dispatcher

      (protocol ? SendChatMessage(jid, text)) pipeTo sender
  }

  def getCredential(jid: String) = {
    val baseJid = StringUtils.parseBareAddress(jid)
    val accessLevel = if (baseJid == Configuration.owner) GlobalAccess else CommonAccess
    val name = StringUtils.parseName(jid)
    Credential(self, accessLevel, None, name, Some(jid))
  }

}
