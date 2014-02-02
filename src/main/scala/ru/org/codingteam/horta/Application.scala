package ru.org.codingteam.horta

import akka.actor.{Props, ActorSystem}
import ru.org.codingteam.horta.core.Core

object Application extends App {
	val system = ActorSystem("CodingteamSystem")
	val core = system.actorOf(Props[Core], name = "core")
}
