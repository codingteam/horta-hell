package ru.org.codingteam.horta.actors

import akka.actor.{ActorLogging, Actor}
import platonus.Network
import ru.org.codingteam.horta.messages.{GeneratedPhrase, GeneratePhrase, InitializeUser, AddPhrase}

class RoomUser extends Actor with ActorLogging {
  var nick: String = null
  val network = new Network()

  def receive = {
    case InitializeUser(userNick) => nick = userNick
    case AddPhrase(phrase) => network.addPhrase(phrase)
    case GeneratePhrase(forNick) => sender ! GeneratedPhrase(forNick, network.doGenerate())
  }
}
