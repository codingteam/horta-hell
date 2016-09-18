package ru.org.codingteam.horta.protocol.xmpp

import ru.org.codingteam.horta.protocol.{GlobalUserId, IProtocol, Participant, RoomId}

import scala.concurrent.Future

class XmppProtocol extends IProtocol {

  override def dispose(): Unit = ???

  override def joinRoom(roomId: RoomId): Unit = ???

  override def getParticipants(roomId: RoomId): Future[Map[String, Participant]] = ???

  override def sendRoomMessage(roomId: RoomId, message: String): Future[Unit] = ???

  override def sendPrivateMessage(userId: GlobalUserId, message: String): Future[Unit] = ???
}
