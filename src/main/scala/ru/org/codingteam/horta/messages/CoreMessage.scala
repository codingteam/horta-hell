package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import ru.org.codingteam.horta.security.{Scope, User, UserRole}

/**
 * Message for the core actor.
 */
@Deprecated
abstract sealed class CoreMessage

@Deprecated
case class RegisterCommand(command: String, role: UserRole, receiver: ActorRef) extends CoreMessage

@Deprecated
case class ProcessCommand(user: User, message: String) extends CoreMessage
