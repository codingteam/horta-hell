package ru.org.codingteam.horta.messages

import org.jivesoftware.smackx.muc.MultiUserChat

abstract class MessengerMessage
case class JoinRoom(roomJID: String) extends MessengerMessage
case class SendMessage(room: MultiUserChat, message: String) extends MessengerMessage

