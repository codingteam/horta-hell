package ru.org.codingteam.horta

import actors.Messenger
import akka.actor.{Props, ActorSystem}
import messages.JoinRoom

object Application extends App {
  val system = ActorSystem("CodingteamSystem")
  val core = system.actorOf(Props[Messenger], name = "core")

  Configuration.rooms foreach { case (roomName, jid) => core ! JoinRoom(jid) }
}
