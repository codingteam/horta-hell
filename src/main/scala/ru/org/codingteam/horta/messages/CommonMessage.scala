package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import ru.org.codingteam.horta.security.User

abstract class CommonMessage
case class InitializePlugin(core: ActorRef, plugins: Map[String, ActorRef]) extends CommonMessage
case class ExecuteCommand(user: User, command: String, arguments: Array[String]) extends CommonMessage
