package ru.org.codingteam.horta.plugins

import akka.actor.ActorRef

/**
 * Trait for plugin that receives the notifications of participant joining and leaving from the room.
 */
trait ParticipantProcessor extends BasePlugin {

  override def receive = {
    case ProcessParticipantJoin(roomJID, participantJID, actor) =>
      processParticipantJoin(roomJID, participantJID, actor)
    case ProcessParticipantLeave(roomJID, participantJID, actor) =>
      processParticipantLeave(roomJID, participantJID, actor)
    case other => super.receive(other)
  }

  override def notifications = super.notifications.copy(participants = true)

  /**
   * Process participant joining the room.
   * @param roomJID JID of the room.
   * @param participantJID JID of the participant.
   * @param actor actor representing the room.
   */
  protected def processParticipantJoin(roomJID: String, participantJID: String, actor: ActorRef)

  /**
   * Process participant leaving the room.
   * @param roomJID JID of the room.
   * @param participantJID JID of the participant.
   * @param actor actor representing the room.
   */
  protected def processParticipantLeave(roomJID: String, participantJID: String, actor: ActorRef)

}
