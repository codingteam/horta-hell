package ru.org.codingteam.horta.actors

import akka.actor.{ActorLogging, Actor}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import platonus.Network
import ru.org.codingteam.horta.messages._
import scala.concurrent.duration._

class RoomUser extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(60 seconds)

  val network = new Network()

  def receive = {
     case AddPhrase(phrase) => {
      if (!phrase.startsWith("$")) {
        network.addPhrase(phrase)
      }
    }

    case GeneratePhrase(forNick) => {
      sender ! GeneratedPhrase(forNick, network.generate())
    }

    case CalculateDiff(forNick, nick1, nick2, roomUser2) => {
      (roomUser2 ? CalculateDiffRequest(forNick, nick1, nick2, network)) pipeTo sender
    }

    case CalculateDiffRequest( forNick, nick1, nick2, network2) => {
      val diff = network.diff(network2)
      sender ! CalculateDiffResponse(forNick, nick1, nick2, diff)
    }
  }
}
