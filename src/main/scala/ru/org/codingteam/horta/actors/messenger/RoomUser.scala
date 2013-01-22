package ru.org.codingteam.horta.actors.messenger

import akka.actor.{ActorLogging, Actor}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import platonus.Network
import ru.org.codingteam.horta.messages._
import scala.concurrent.duration._
import java.util.Locale

class RoomUser extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(60 seconds)

  val network = new Network()
  var lastMessage: Option[String] = None

  def receive = {
    case UserPhrase(message) => {
      if (addPhrase(message)) {
        lastMessage = Some(message)
      }
    }

    case AddPhrase(phrase) => {
      addPhrase(phrase)
    }

    case GeneratePhrase(forNick, allCaps) => {
      val phrase = network.generate()
      sender ! GeneratedPhrase(forNick, if (allCaps) phrase.toUpperCase(Locale.ROOT) else phrase)
    }

    case ReplaceRequest(from, to) => {
      lastMessage match {
        case Some(message) => sender ! ReplaceResponse(message.replace(from, to))
        case None          => sender ! ReplaceResponse("No messages for you, sorry.")
      }
    }

    case CalculateDiff(forNick, nick1, nick2, roomUser2) => {
      (roomUser2 ? CalculateDiffRequest(forNick, nick1, nick2, network)) pipeTo sender
    }

    case CalculateDiffRequest( forNick, nick1, nick2, network2) => {
      val diff = network.diff(network2)
      sender ! CalculateDiffResponse(forNick, nick1, nick2, diff)
    }
  }

  def addPhrase(phrase: String) = {
    if (!phrase.startsWith("$") && !phrase.startsWith("s/")) {
      network.addPhrase(phrase)
      true
    } else {
      false
    }
  }
}
