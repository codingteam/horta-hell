package ru.org.codingteam.horta.messages

import org.jivesoftware.smack.packet.{Message, Presence}

abstract sealed class RoomMessage

case class UserMessage(message: Message) extends RoomMessage

case class UserPresence(nick: String, presenceType: Presence.Type) extends RoomMessage

case class ParsedPhrase(nick: String, message: String) extends RoomMessage

case class GenerateCommand(jid: String, command: String, arguments: Array[String]) extends RoomMessage

case class ReplaceCommand(jid: String, arguments: Array[String]) extends RoomMessage