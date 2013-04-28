package ru.org.codingteam.horta.messages

import org.jivesoftware.smack.Chat

abstract sealed class MessengerMessage

case class Reconnect() extends MessengerMessage

case class JoinRoom(roomJID: String) extends MessengerMessage

case class SendMucMessage(toJid: String, message: String) extends MessengerMessage

case class SendChatMessage(toJid: String, message: String) extends MessengerMessage

case class ChatOpened(chat: Chat) extends MessengerMessage

