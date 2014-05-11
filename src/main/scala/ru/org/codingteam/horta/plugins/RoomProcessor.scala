package ru.org.codingteam.horta.plugins

import akka.actor.ActorRef

/**
 * Trait for plugin that receives the notifications of room joining.
 */
trait RoomProcessor extends BasePlugin {

  override def receive = {
    case ProcessRoomJoin(roomJID, actor) =>
      processRoomJoin(roomJID, actor)
    case ProcessRoomLeave(roomJID) =>
      processRoomLeave(roomJID)
    case other => super.receive(other)
  }

  override def notifications = super.notifications.copy(rooms = true)

  /**
   * Process joining the room.
   * @param roomJID JID of the room.
   * @param actor actor representing the room.
   */
  protected def processRoomJoin(roomJID: String, actor: ActorRef)

  /**
   * Process leaving the room.
   * @param roomJID JID of the room.
   */
  protected def processRoomLeave(roomJID: String)

}
