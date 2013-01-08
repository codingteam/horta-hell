package org.ru.codingteam.horta.messages

abstract class RoomMessage
case class UserMessage(jid: String, message: String) extends RoomMessage
