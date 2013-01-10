package ru.org.codingteam.horta

import akka.actor.{Props, ActorSystem}
import messages.JoinRoom

object Application extends App {
  val system = ActorSystem("CodingteamSystem")
  val core = system.actorOf(Props[CoreActor], name = "core")

  Configuration.rooms foreach { case (roomName, jid) => core ! JoinRoom(jid) }
}
