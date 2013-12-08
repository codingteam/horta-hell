package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{Actor, ActorLogging, ActorRef}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.{GlobalAccess, CommonAccess, Credential}
import ru.org.codingteam.horta.messages.UserMessage
import ru.org.codingteam.horta.Configuration
import org.jivesoftware.smack.util.StringUtils

class PrivateMessageHandler(val protocol: ActorRef) extends Actor with ActorLogging {

  val core = context.actorSelection("/user/core")

  def receive() = {
		case UserMessage(message) => {
			val jid = message.getFrom
			val text = message.getBody

      val credential = getCredential(jid)

      core ! CoreMessage(credential, text)
		}

		case SendResponse(credential, text) => {
      val jid = credential.id.get.asInstanceOf[String]
      protocol ! SendChatMessage(jid, text)
		}
	}

  def getCredential(jid: String) = {
    val baseJid = StringUtils.parseBareAddress(jid)
    val accessLevel = if (baseJid == Configuration.owner) GlobalAccess else CommonAccess
    val name = StringUtils.parseName(jid)
    Credential(self, accessLevel, name, Some(jid))
  }
}
