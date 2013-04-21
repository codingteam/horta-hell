package ru.org.codingteam.horta.actors.core

import ru.org.codingteam.horta.security.UserRole
import akka.actor.ActorRef

@Deprecated
case class Command(name: String, role: UserRole, targetPlugin: ActorRef)
