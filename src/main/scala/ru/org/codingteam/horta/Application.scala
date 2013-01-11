package ru.org.codingteam.horta

import actors.MessageActor
import akka.actor.{Props, ActorSystem}
import messages.JoinRoom

object Application extends App {
  val system = ActorSystem("CodingteamSystem")
  val core = system.actorOf(Props[MessageActor], name = "core")

  Configuration.rooms foreach { case (roomName, jid) => core ! JoinRoom(jid) }
}
