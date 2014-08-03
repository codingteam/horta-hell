package ru.org.codingteam.horta.plugins

import akka.actor.ActorRef
import org.joda.time.DateTime
import ru.org.codingteam.horta.messages.LeaveReason

/**
 * Trait for plugin that receives the notifications of participant joining and leaving from the room.
 */
trait ParticipantProcessor extends BasePlugin {

  override def receive = {
    case ProcessParticipantJoin(time, roomJID, participantJID, actor) =>
      processParticipantJoin(time, roomJID, participantJID, actor)
    case ProcessParticipantLeave(time, roomJID, participantJID, reason, actor) =>
      processParticipantLeave(time, roomJID, participantJID, reason, actor)
    case other => super.receive(other)
  }

  override def notifications = super.notifications.copy(participants = true)

  /**
   * Process participant joining the room.
   * @param time time of an event.
   * @param roomJID JID of the room.
   * @param participantJID JID of the participant.
   * @param actor actor representing the room.
   */
  protected def processParticipantJoin(time: DateTime, roomJID: String, participantJID: String, actor: ActorRef)

  /**
   * Process participant leaving the room.
   * @param time time of an event.
   * @param roomJID JID of the room.
   * @param participantJID JID of the participant.
   * @param reason event reason.
   * @param actor actor representing the room.
   */
  protected def processParticipantLeave(time: DateTime,
                                        roomJID: String,
                                        participantJID: String,
                                        reason: LeaveReason,
                                        actor: ActorRef)

}
