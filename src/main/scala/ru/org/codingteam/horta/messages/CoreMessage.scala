package ru.org.codingteam.horta.messages

import ru.org.codingteam.horta.security.{User, UserRole}
import akka.actor.ActorRef

abstract class CoreMessage
case class RegisterCommand(command: String, role: UserRole, receiver: ActorRef) extends CoreMessage
case class ProcessCommand(user: User, message: String) extends CoreMessage
