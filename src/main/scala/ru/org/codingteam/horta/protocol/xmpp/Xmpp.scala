package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import org.jivesoftware.smack.util.StringUtils
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages.CoreMessage
import ru.org.codingteam.horta.protocol.RoomUserId
import ru.org.codingteam.horta.security.{CommonAccess, Credential, GlobalAccess}

import scala.concurrent.duration._

/**
 * Actor to communicate through Extensible Messaging and Presence Protocol aka Jabber.
 */
class Xmpp extends Actor with ActorLogging {

  private val core = context.actorSelection("/user/core")

  private val connectionParams = ConnectionParameters(Configuration.server, Configuration.login, Configuration.password)
  private val supervisor = BackoffSupervisor.props(
    Backoff.onStop(
      Props(classOf[XmppConnection], connectionParams).withDispatcher("pinned-dispatcher"),
      childName = "connection",
      minBackoff = 5.seconds,
      maxBackoff = 120.seconds,
      randomFactor = 0.05
    ))
  private val connection = context.actorOf(supervisor, "connection-supervisor")

  private var roomLocales: Map[String, LocaleDefinition] = Map() // TODO: Fill these. ~ F

  override def receive: Receive = {
    // External messages:
    // TODO: Implement these; see XmppProtocolWrapper for implementation spec. ~ F
    case JoinRoom(room, locale) => ???
    case GetParticipants() => ???
    case SendRoomMessage(roomId, text) => ???
    case SendPrivateRoomMessage(userId, text) => ???
    case SendDirectMessage(userId, text) => ???

    // Messages from XmppConnection:
    case message: PrivateMessageReceived => core ! toCoreMessage(message)
    case message: RoomMessageReceived => core ! toCoreMessage(message)
  }

  private def toCoreMessage(message: PrivateMessageReceived): CoreMessage = {
    val PrivateMessageReceived(time, userId, text) = message

    val jid = StringUtils.parseBareAddress(userId.id)
    val accessLevel = if (jid == Configuration.owner) GlobalAccess else CommonAccess
    val name = StringUtils.parseName(jid)
    val credential = Credential(self, Configuration.defaultLocalization, accessLevel, None, name, Some(jid))

    CoreMessage(time, credential, text)
  }

  private def toCoreMessage(message: RoomMessageReceived): CoreMessage = {
    val RoomMessageReceived(time, roomUserId, text) = message
    val RoomUserId(roomId, userId) = roomUserId

    val accessLevel = CommonAccess // TODO: Consult participant map to know the access level.
    val locale = roomLocales(roomId.id)
    val credential = Credential(self, locale, accessLevel, Some(roomId.id), Xmpp.nickByJid(userId), Some(userId))

    CoreMessage(time, credential, text)
  }
}

object Xmpp {

  def nickByJid(jid: String) = {
    StringUtils.parseResource(jid)
  }
}
