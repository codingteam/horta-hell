package ru.org.codingteam.horta.messages

import org.jivesoftware.smackx.muc.MultiUserChat

abstract class CoreMessage
case class JoinRoom(roomJID: String) extends CoreMessage
case class SendMessage(room: MultiUserChat, message: String) extends CoreMessage

