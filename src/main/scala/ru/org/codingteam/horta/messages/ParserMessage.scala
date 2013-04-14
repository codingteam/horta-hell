package ru.org.codingteam.horta.messages

abstract sealed class ParserMessage

case class DoParsing(roomName: String, userName: String) extends ParserMessage
