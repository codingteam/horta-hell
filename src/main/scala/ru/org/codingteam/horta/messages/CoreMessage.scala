package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import ru.org.codingteam.horta.actors.core.ParserMode
import ru.org.codingteam.horta.security.{User, UserRole}

abstract class CoreMessage
case class RegisterCommand(mode: ParserMode, command: String, role: UserRole, receiver: ActorRef) extends CoreMessage
case class ProcessCommand(user: User, message: String) extends CoreMessage
