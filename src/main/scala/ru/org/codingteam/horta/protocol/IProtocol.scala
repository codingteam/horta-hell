package ru.org.codingteam.horta.protocol

import ru.org.codingteam.horta.protocol.jabber.{Affiliation, Role}

import scala.concurrent.Future

case class GlobalUserId(id: String)
case class RoomId(id: String)
case class RoomUserId(id: String)

case class Participant(id: RoomUserId, affiliation: Affiliation, role: Role)

trait IProtocol {

  /**
   * Close the network connection and free all the resources used by protocol implementation.
   */
  def dispose(): Unit

  /**
   * Set up the protocol to join the room. The protocol will try to rejoin the room on errors with protocol-defined
   * interval.
   *
   * @param roomId room identifier.
   */
  def joinRoom(roomId: RoomId): Unit

  /**
   * Get room participants.
   * @param roomId room identifier.
   * @return (name ⇒ participant) map
   */
  def getParticipants(roomId: RoomId): Future[Map[String, Participant]]

  /**
   * Send public message to the room.
   *
   * @param roomId room identifier.
   * @param message message text.
   */
  def sendRoomMessage(roomId: RoomId, message: String): Future[Unit]

  /**
   * Send private message to the room user.
   *
   * @param userId user identifier.
   * @param message message text.
   */
  def sendPrivateRoomMessage(userId: RoomUserId, message: String): Future[Unit]

  /**
   * Send a message to the protocol user.
   *
   * @param userId user identifier.
   * @param message message text.
   */
  def sendDirectMessage(userId: GlobalUserId, message: String): Future[Unit]
}
