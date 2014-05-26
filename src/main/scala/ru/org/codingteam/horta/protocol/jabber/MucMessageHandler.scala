package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.util.regex.Pattern
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.{SendMucMessage, SendPrivateMessage, SendPrivateResponse, SendResponse}
import ru.org.codingteam.horta.security._
import scala.concurrent.duration._
import scala.Some

/**
 * Multi user chat message handler.
 */
class MucMessageHandler(val protocol: ActorRef, val roomJID: String) extends Actor with ActorLogging {

  val core = context.actorSelection("/user/core")

  var participants = Map[String, Affinity]()

  override def preStart() {
    super.preStart()
    core ! CoreRoomJoin(Clock.now, roomJID, self)
  }

  override def postStop() {
    core ! CoreRoomLeave(Clock.now, roomJID)
    super.postStop()
  }

  def receive = {
    case UserJoined(participant, affilationName) =>
      val affiliation = affilationName match {
        case "owner" => Owner
        case "admin" => Admin
        case _ => User
      }

      addParticipant(participant, affiliation)
      log.info(s"$participant joined as $affiliation")

    case UserLeft(participant) =>
      removeParticipant(participant)
      log.info(s"$participant left")

    case OwnershipGranted(participant) =>
      participants = participants.updated(participant, Owner)
      log.info(s"$participant became an owner")

    case OwnershipRevoked(participant) =>
      participants = participants.updated(participant, User)
      log.info(s"$participant ceased to be an owner")

    case AdminGranted(participant) =>
      participants = participants.updated(participant, Admin)
      log.info(s"$participant became an admin")

    case AdminRevoked(participant) =>
      participants = participants.updated(participant, User)
      log.info(s"$participant ceased to be an admin")

    case NicknameChanged(participant, newNick) =>
      val newParticipant = jidByNick(newNick)
      val access = participants(participant)
      removeParticipant(participant)
      addParticipant(newParticipant, access)
      log.info(s"$participant changed nick to $newNick")

    case UserMessage(message) =>
      val jid = message.getFrom
      val text = message.getBody

      if (text != null) {
        val credential = getCredential(jid)
        core ! CoreMessage(Clock.now, credential, text)
      }

    case SendResponse(credential, text) =>
      sendMessage(credential, text, false)

    case SendPrivateResponse(credential, text) =>
      sendMessage(credential, text, true)
  }

  def getCredential(jid: String) = {
    // TODO: Use admin access to know the real JID if possible.
    // TODO: If user known to be an owner - give him the GlobalAccess level.
    val accessLevel = participants.get(jid) match {
      case None =>
        log.warning(s"Cannot find participant $jid in the participant list")
        CommonAccess
      case Some(Owner) | Some(Admin) => RoomAdminAccess
      case Some(User) => CommonAccess
    }

    Credential(self, accessLevel, Some(roomJID), nickByJid(jid), Some(jid))
  }

  def jidByNick(nick: String) = s"$roomJID/$nick"

  def nickByJid(jid: String) = {
    val args = jid.split('/')
    if (args.length > 1) {
      args(1)
    } else {
      args(0)
    }
  }

  def sendMessage(credential: Credential, text: String, isPrivate: Boolean) {
    val name = credential.name
    val response = prepareResponse(name, text, isPrivate)
    val message = if (isPrivate) SendPrivateMessage(roomJID, name, response) else SendMucMessage(roomJID, response)

    implicit val timeout = Timeout(60.seconds)
    import context.dispatcher

    (protocol ? message) pipeTo sender
  }

  def prepareResponse(recipient: String, text: String, isPrivate: Boolean) = {
    if (isPrivate) {
      text
    } else {
      var message = text
      for (nick <- participants.keys.map(nickByJid)) {
        if (nick != recipient && nick.length > 0) {
          val quoted = Pattern.quote(nick)
          val pattern = s"(?<=\\W|^)$quoted(?=\\W|$$)"
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

  private def addParticipant(participantJID: String, affinity: Affinity) {
    val oldSize = participants.size
    participants += participantJID -> affinity
    val changed = oldSize != participants.size

    if (changed) {
      core ! CoreParticipantJoined(Clock.now, roomJID, participantJID, self)
    }
  }

  private def removeParticipant(participantJID: String) {
    val oldSize = participants.size
    participants -= participantJID
    val changed = oldSize != participants.size

    if (changed) {
      core ! CoreParticipantLeft(Clock.now, roomJID, participantJID, self)
    }
  }

}
