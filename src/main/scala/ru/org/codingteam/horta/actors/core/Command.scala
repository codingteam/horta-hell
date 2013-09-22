package ru.org.codingteam.horta.actors.core

import akka.actor.ActorRef
import ru.org.codingteam.horta.security.AccessLevel

@Deprecated
case class Command(level: AccessLevel, name: String, targetPlugin: ActorRef)
