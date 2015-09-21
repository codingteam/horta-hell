package ru.org.codingteam.horta.protocol.jabber

import java.util.regex.Pattern

import akka.actor.ActorRef
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol._
import ru.org.codingteam.horta.security._

import scala.concurrent.duration._

/**
 * Multi user chat message handler.
 */
class MucMessageHandler(locale: LocaleDefinition,
                        protocol: ActorRef,
                        roomJID: String,
                        nickname: String) extends LocationActor(locale) {

  val core = context.actorSelection("/user/core")

  val participant = Protocol.Participant(s"$roomJID/$nickname", User, Participant) // TODO: Get own role and affiliation
  var participants: Protocol.ParticipantCollection = Map(participant.jid -> participant)

  override def preStart() {
    super.preStart()
    core ! CoreRoomJoin(Clock.now, roomJID, self)
  }

  override def postStop() {
    core ! CoreRoomLeave(Clock.now, roomJID)
    super.postStop()
  }

  override def receive = {
    case UserJoined(participant, affiliation, role) =>
      addParticipant(Protocol.Participant(participant, affiliation, role))
      log.info(s"$participant joined as $affiliation with role $role")

    case UserLeft(participant, reason) =>
      removeParticipant(participant, reason)
      log.info(s"$participant left, reason: $reason")

    case OwnershipGranted(participant) =>
      participants = participants.updated(participant, Protocol.Participant(participant, Owner, Moderator))
      log.info(s"$participant became an owner")

    case OwnershipRevoked(participant) =>
      participants = participants.updated(participant, Protocol.Participant(participant, User, Participant))
      log.info(s"$participant ceased to be an owner")

    case AdminGranted(participant) =>
      participants = participants.updated(participant, Protocol.Participant(participant, Admin, Participant))
      log.info(s"$participant became an admin")

    case AdminRevoked(participant) =>
      participants = participants.updated(participant, Protocol.Participant(participant, User, Participant))
      log.info(s"$participant ceased to be an admin")

    case NicknameChanged(participant, newNick) =>
      val newParticipant = jidByNick(newNick)
      val oldParticipant = participants(participant)
      removeParticipant(participant, UserRenamed(newNick))
      addParticipant(oldParticipant.copy(jid = newParticipant))
      log.info(s"$participant changed nick to $newNick")

    case UserMessage(message) =>
      val jid = message.getFrom
      val text = message.getBody

      (jid, text) match {
        case (`roomJID`, _) =>
          core ! CoreRoomTopicChanged(Clock.now, roomJID, text, self)
        case (_, _) if text != null =>
          val credential = getCredential(jid)
          core ! CoreMessage(Clock.now, credential, text)
        case _ =>
      }

    case SendResponse(credential, text) =>
      sendMessage(credential, text, false)

    case SendPrivateResponse(credential, text) =>
      sendMessage(credential, text, true)

    case GetParticipants =>
      sender ! participants

    case other =>
      super.receive(other)
  }

  def getCredential(jid: String) = {
    // TODO: Use admin access to know the real JID if possible.
    // TODO: If user known to be an owner - give him the GlobalAccess level.
    val accessLevel = participants.get(jid) match {
      case None =>
        log.warning(s"Cannot find participant $jid in the participant list")
        CommonAccess
      case Some(p) => p.affiliation match {
        case Owner | Admin => RoomAdminAccess
        case _ => CommonAccess
      }
    }

    Credential(self, locale, accessLevel, Some(roomJID), Protocol.nickByJid(jid), Some(jid))
  }

  def jidByNick(nick: String) = s"$roomJID/$nick"

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
      for (nick <- participants.keys.map(Protocol.nickByJid)) {
        if (nick != recipient) {
          val quoted = Pattern.quote(nick)
          val pattern = s"(?<=\\W|^)$quoted(?=\\W|$$)"
          val replacement = MucMessageHandler.getNickReplacement(nick)
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

  private def addParticipant(participant: Protocol.Participant) {
    val oldSize = participants.size
    participants += participant.jid -> participant
    val changed = oldSize != participants.size

    if (changed) {
      core ! CoreParticipantJoined(Clock.now, roomJID, participant.jid, self)
    }
  }

  private def removeParticipant(participantJID: String, reason: LeaveReason) {
    val oldSize = participants.size
    participants -= participantJID
    val changed = oldSize != participants.size

    if (changed) {
      core ! CoreParticipantLeft(Clock.now, roomJID, participantJID, reason, self)
    }
  }
}

object MucMessageHandler {

  private val vowels = "(?i)[aeiouаоэиуыеёюя]".r // TODO: Use some phonetic definition instead of hardcode
  private val separator = "-"

  def getNickReplacement(nick: String): String = {
    if (nick.length <= 2) {
      nick
    } else {
      val matched = vowels.findAllMatchIn(nick)
      val position = matched.collectFirst({ case m if m.start > 0 && m.end < nick.length =>
        m.start
      }).getOrElse(1)

      nick.substring(0, position) + separator + nick.substring(position + 1)
    }
  }
}
