package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{Backoff, BackoffSupervisor, ask, pipe}
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.jabber._
import ru.org.codingteam.horta.protocol.{RoomId, RoomUserId}
import ru.org.codingteam.horta.security.{CommonAccess, Credential, GlobalAccess}

import scala.concurrent.duration._

/**
 * Actor to communicate through Extensible Messaging and Presence Protocol aka Jabber.
 */
class Xmpp extends Actor with ActorLogging {

  import context.dispatcher
  import ru.org.codingteam.horta.protocol.xmpp.Xmpp.XmppRoomDescriptor

  implicit val timeout = Timeout(5.minutes)

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

  private var rooms: Map[String, XmppRoomDescriptor] = Map()

  override def receive: Receive = {
    // External messages:
    // TODO: Implement these; see XmppProtocolWrapper for implementation spec. ~ F
    case JoinRoom(roomJID, locale, botName, greeting) =>
      val descriptor = XmppRoomDescriptor(locale, botName, greeting)
      rooms += roomJID -> descriptor
      connectRoom(roomJID, descriptor)
    case m: GetParticipants => pipe(connection.ask(m)).to(sender)
    case SendRoomMessage(roomId, text) => ???
    case SendPrivateRoomMessage(userId, text) => ???
    case SendDirectMessage(userId, text) => ???

    // Messages from XmppConnection:
    case ConnectionReady() => initializeConnection()
    case message: PrivateMessageReceived => core ! toCoreMessage(message)
    case message: RoomMessageReceived => core ! toCoreMessage(message)

    // Messages from MucParticipantStatusListener:
    // TODO: Implement those; see MucMessageHandler. ~ F
    case UserJoined(participant, affiliation, role) => ???
    case UserLeft(participant, reason) => ???
    case OwnershipGranted(participant) => ???
    case OwnershipRevoked(participant) => ???
    case AdminGranted(participant) => ???
    case AdminRevoked(participant) => ???
    case NicknameChanged(participant, newNick) => ???
  }

  private def initializeConnection(): Unit = {
    rooms.foreach({ case (roomId, descriptor) => connectRoom(roomId, descriptor) })
  }

  private def connectRoom(roomId: String, descriptor: XmppRoomDescriptor): Unit = {
    connection ! ConnectRoom(RoomId(roomId), descriptor.nickname, descriptor.greeting)
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
    val locale = rooms(roomId.id).locale
    val credential = Credential(self, locale, accessLevel, Some(roomId.id), Xmpp.nickByJid(userId), Some(userId))

    CoreMessage(time, credential, text)
  }
}

object Xmpp {

  private case class XmppRoomDescriptor(locale: LocaleDefinition, nickname: String, greeting: Option[String])

  def nickByJid(jid: String) = {
    StringUtils.parseResource(jid)
  }

  def decodeAffiliation(affiliationName: String) = affiliationName match {
    case "owner" => Owner
    case "admin" => Admin
    case "none" => NoneAffiliation
    case _ => User // TODO: Check the real value for user if it exist. Currently I have no time to experiment. ~ F
  }

  def decodeRole(roleName: String) = roleName match {
    case "moderator" => Moderator
    case "participant" => Participant
  }
}
