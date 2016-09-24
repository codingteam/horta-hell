package ru.org.codingteam.horta.protocol.jabber

import akka.actor.ActorRef
import org.jivesoftware.smackx.muc.{DefaultParticipantStatusListener, MultiUserChat}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.xmpp.Xmpp

class MucParticipantStatusListener(muc: MultiUserChat, room: ActorRef) extends DefaultParticipantStatusListener {

  // TODO: Test for changing the user status from visitor to participant and back.
  override def joined(participant: String) {
    val occupant = muc.getOccupant(participant)
    val affiliation = Xmpp.decodeAffiliation(occupant.getAffiliation)
    val role = Xmpp.decodeRole(occupant.getRole)

    room ! UserJoined(participant, affiliation, role)
  }

  override def left(participant: String) {
    room ! UserLeft(participant)
  }

  override def ownershipGranted(participant: String) {
    room ! OwnershipGranted(participant)
  }

  override def ownershipRevoked(participant: String) {
    room ! OwnershipRevoked(participant)
  }

  override def adminGranted(participant: String) {
    room ! AdminGranted(participant)
  }

  override def adminRevoked(participant: String) {
    room ! AdminRevoked(participant)
  }

  override def nicknameChanged(participant: String, newNickname: String) {
    room ! NicknameChanged(participant, newNickname)
  }

  override def kicked(participant: String, actor: String, reason: String) {
    room ! UserLeft(participant, UserKickedReason(reason))
  }

  override def banned(participant: String, actor: String, reason: String) {
    room ! UserLeft(participant, UserBannedReason(reason))
  }

}
