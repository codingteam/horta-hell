package ru.org.codingteam.horta.messages

import org.jivesoftware.smack.packet.Message

case class UserJoined(participant: String, affilation: String)
case class UserLeft(participant: String)
case class OwnershipGranted(participant: String)
case class OwnershipRevoked(participant: String)
case class AdminGranted(participant: String)
case class AdminRevoked(participant: String)
case class NicknameChanged(participant: String, newNickname: String)
case class UserMessage(message: Message)
case class GenerateCommand(jid: String, command: String, arguments: Array[String])
case class ReplaceCommand(jid: String, arguments: Array[String])