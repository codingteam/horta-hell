package ru.org.codingteam.horta.messages

import ru.org.codingteam.horta.security.User

abstract class CommonMessage
case class ExecuteCommand(user: User, command: String, arguments: Array[String]) extends CommonMessage
