package ru.org.codingteam.horta.messages

import ru.org.codingteam.horta.security.User

sealed class CommonMessage

@Deprecated
case class ExecuteCommand(user: User, command: String, arguments: Array[String]) extends CommonMessage

case object PositiveReply extends CommonMessage
