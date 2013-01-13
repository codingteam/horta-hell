package ru.org.codingteam.horta.actors

import akka.actor.{ActorRef, ActorLogging, Actor}
import platonus.Network
import ru.org.codingteam.horta.messages._

class RoomUser extends Actor with ActorLogging {
  var messenger: ActorRef = null

  val network = new Network()

  def receive = {
    case InitializeUser(messenger) =>{
      this.messenger = messenger
    }

    case AddPhrase(phrase) => {
      if (!phrase.startsWith("$")) {
        network.addPhrase(phrase)
      }
    }

    case GeneratePhrase(forNick) => {
      messenger ! GeneratedPhrase(forNick, network.generate())
    }

    case CalculateDiff(forNick, nick1, nick2, roomUser2) => {
      roomUser2 ! CalculateDiffRequest(forNick, nick1, nick2, network)
    }

    case CalculateDiffRequest(forNick, nick1, nick2, network2) => {
      val diff = network.diff(network2)
      messenger ! CalculateDiffResponse(forNick, nick1, nick2, diff)
    }
  }
}
