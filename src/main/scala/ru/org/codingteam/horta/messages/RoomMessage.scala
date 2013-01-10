package ru.org.codingteam.horta.messages

import org.jivesoftware.smackx.muc.MultiUserChat

abstract class RoomMessage
case class UserMessage(jid: String, message: String) extends RoomMessage
case class Initialize(room: MultiUserChat) extends RoomMessage
