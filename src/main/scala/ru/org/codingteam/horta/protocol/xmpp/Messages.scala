package ru.org.codingteam.horta.protocol.xmpp

import ru.org.codingteam.horta.protocol.{GlobalUserId, RoomId, RoomUserId}

private case class JoinRoom(room: RoomId)
private case class GetParticipants()
private case class SendRoomMessage(roomId: RoomId, text: String)
private case class SendPrivateRoomMessage(userId: RoomUserId, text: String)
private case class SendDirectMessage(userId: GlobalUserId, text: String)
