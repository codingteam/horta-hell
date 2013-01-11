package ru.org.codingteam.horta.messages

abstract class MessengerMessage
case class JoinRoom(roomJID: String) extends MessengerMessage
case class SendMessage(roomName: String, message: String) extends MessengerMessage

