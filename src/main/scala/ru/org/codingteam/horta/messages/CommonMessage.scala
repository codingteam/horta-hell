package ru.org.codingteam.horta.messages

import ru.org.codingteam.horta.security.Credential

sealed class CommonMessage

@Deprecated
case class ExecuteCommand(user: Credential, command: String, arguments: Array[String]) extends CommonMessage

case object PositiveReply extends CommonMessage
