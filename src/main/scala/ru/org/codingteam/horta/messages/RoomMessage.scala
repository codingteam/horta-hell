package ru.org.codingteam.horta.messages

import org.jivesoftware.smack.packet.{Message, Presence}

abstract sealed class RoomMessage

case class UserMessage(message: Message) extends RoomMessage

case class UserPresence(presence: Presence) extends RoomMessage

case class ParsedPhrase(nick: String, message: String) extends RoomMessage

case class GeneratedPhrase(forNick: String, phrase: String) extends RoomMessage

case class CalculateDiffResponse(forNick: String, nick1: String, nick2: String, diff: Double) extends RoomMessage

case class GenerateCommand(jid: String, command: String, arguments: Array[String]) extends RoomMessage

case class ReplaceCommand(jid: String, arguments: Array[String]) extends RoomMessage

case class ReplaceResponse(message: String) extends RoomMessage

case class DiffCommand(jid: String, arguments: Array[String]) extends RoomMessage

case class PetResponse(message: String) extends RoomMessage
