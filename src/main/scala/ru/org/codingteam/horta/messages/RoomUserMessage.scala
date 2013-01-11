package ru.org.codingteam.horta.messages

abstract class RoomUserMessage
case class InitializeUser(userNick: String) extends RoomUserMessage
case class AddPhrase(phrase: String) extends RoomUserMessage
case class GeneratePhrase(forNick: String) extends RoomUserMessage
