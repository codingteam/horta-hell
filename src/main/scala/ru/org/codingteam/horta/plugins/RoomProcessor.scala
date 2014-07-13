package ru.org.codingteam.horta.plugins

import akka.actor.ActorRef
import org.joda.time.DateTime

/**
 * Trait for plugin that receives the notifications of room joining.
 */
trait RoomProcessor extends BasePlugin {

  override def receive = {
    case ProcessRoomJoin(time, roomJID, actor) =>
      processRoomJoin(time, roomJID, actor)
    case ProcessRoomLeave(time, roomJID) =>
      processRoomLeave(time, roomJID)
    case ProcessRoomTopicChange(time, roomId, text) =>
      processRoomTopicChange(time, roomId, text)
    case other => super.receive(other)
  }

  override def notifications = super.notifications.copy(rooms = true)

  /**
   * Process joining the room.
   * @param time time of an event.
   * @param roomJID JID of the room.
   * @param actor actor representing the room.
   */
  protected def processRoomJoin(time: DateTime, roomJID: String, actor: ActorRef)

  /**
   * Process leaving the room.
   * @param time time of an event.
   * @param roomJID JID of the room.
   */
  protected def processRoomLeave(time: DateTime, roomJID: String)

  /**
   * Process room topic change.
   * @param time time of an event.
   * @param roomId room identifier.
   * @param text new topic text.
   */
  protected def processRoomTopicChange(time: DateTime, roomId: String, text: String) {}

}
