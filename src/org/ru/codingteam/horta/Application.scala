package org.ru.codingteam.horta

import akka.actor.{Props, ActorSystem}

object Application extends App {
  val system = ActorSystem("CodingteamSystem")
  val core = system.actorOf(Props[CoreActor], name = "core")
}
