package ru.org.codingteam.horta.messages

import org.jivesoftware.smack.packet.Message

abstract sealed class LeaveReason {
  def text: String
}

case class UserLeftReason(text: String) extends LeaveReason
case class UserKickedReason(text: String) extends LeaveReason
case class UserBannedReason(text: String) extends LeaveReason
case class UserRenamed(newNick: String) extends LeaveReason {
  override def text = "renamed to " + newNick
}

case class UserJoined(participant: String, affilation: String)
case class UserLeft(participant: String, reason: LeaveReason = UserLeftReason(""))
case class OwnershipGranted(participant: String)
case class OwnershipRevoked(participant: String)
case class AdminGranted(participant: String)
case class AdminRevoked(participant: String)
case class NicknameChanged(participant: String, newNickname: String)
case class UserMessage(message: Message)
case class GenerateCommand(jid: String, command: String, arguments: Array[String])
case class ReplaceCommand(jid: String, arguments: Array[String])