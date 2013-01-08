package org.ru.codingteam.horta.messages

abstract class CoreMessage
case class JoinRoom(roomJID: String) extends CoreMessage

