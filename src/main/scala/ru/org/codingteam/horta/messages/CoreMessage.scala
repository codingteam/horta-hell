package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import ru.org.codingteam.horta.security.{User, UserRole}

abstract class CoreMessage

case class RegisterCommand(command: String, role: UserRole, receiver: ActorRef) extends CoreMessage

case class ProcessCommand(user: User, message: String) extends CoreMessage
