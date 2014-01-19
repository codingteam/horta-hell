package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{ActorRef, Actor, ActorLogging}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.CommonAccess
import ru.org.codingteam.horta.messages.UserMessage
import ru.org.codingteam.horta.messages.CoreMessage
import scala.Some
import ru.org.codingteam.horta.security.Credential
import ru.org.codingteam.horta.messages.UserPresence
import java.util.regex.Pattern

/**
 * Multi user chat message handler.
 */
class MucMessageHandler(val protocol: ActorRef, val roomJid: String) extends Actor with ActorLogging {

  val core = context.actorSelection("/user/core")

  var userNicks = Set[String]()

  def receive = {
    case UserMessage(message) => {
      val jid = message.getFrom
      val text = message.getBody

      val credential = getCredential(jid)
      core ! CoreMessage(credential, text)
    }

    case UserPresence(jid, presenceType) => {
      // TODO: Handle unavailable presences.
      val nick = nickByJid(jid)
      userNicks += nick
    }

    case SendResponse(credential, text) => {
      val response = prepareResponse(credential.name, text)
      sendMessage(response)
    }
  }

  def getCredential(jid: String) = {
    // TODO: Use admin access to know the real JID if possible.
    // TODO: If user known to be an owner - give him the GlobalAccess level.
    val accessLevel = CommonAccess // TODO: Get real access level.
    Credential(self, accessLevel, Some(roomJid), nickByJid(jid), Some(jid))
  }

  def nickByJid(jid: String) = {
    val args = jid.split('/')
    if (args.length > 1) {
      args(1)
    } else {
      args(0)
    }
  }

  def sendMessage(message: String) {
    protocol ! SendMucMessage(roomJid, message)
  }

  def prepareResponse(recipient: String, text: String) = {
    var message = text
    for (nick <- userNicks) {
      if (nick != recipient && nick.length > 0) {
        val quoted = Pattern.quote(nick)
        val pattern = s"\\b$quoted\\b"
        val replacement = nick.substring(0, 1) + "…"
        message = message.replaceAll(pattern, replacement)
      }
    }

    if (recipient.isEmpty) {
      message
    } else {
      s"$recipient: $message"
    }
  }
}
