package ru.org.codingteam.horta.messages

abstract class ParserMessage
case class DoParsing(roomName: String) extends ParserMessage
