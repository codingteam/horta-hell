package ru.org.codingteam.horta.messages

abstract class RoomMessage
case class UserMessage(jid: String, message: String) extends RoomMessage
case class ParsedPhrase(nick: String, message: String) extends RoomMessage
case class GeneratedPhrase(forNick: String, phrase: String) extends RoomMessage
case class CalculateDiffResponse(forNick: String, nick1: String, nick2: String, diff: Double) extends RoomMessage
case class GenerateCommand(jid: String, command: String) extends RoomMessage
case class DiffCommand(jid: String, arguments: Array[String]) extends RoomMessage
