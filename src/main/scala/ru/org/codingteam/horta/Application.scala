package ru.org.codingteam.horta

import actors.core.Core
import akka.actor.{Props, ActorSystem}

object Application extends App {
	val system = ActorSystem("CodingteamSystem")
	val core = system.actorOf(Props[Core], name = "core")
}
