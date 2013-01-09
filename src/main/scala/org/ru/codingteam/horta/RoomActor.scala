package org.ru.codingteam.horta

import akka.actor.{Props, Actor, ActorLogging}
import messages.{Initialize, SendMessage, UserMessage}
import platonus.Network
import org.jivesoftware.smackx.muc.MultiUserChat

class RoomActor extends Actor with ActorLogging {
  var room: MultiUserChat = null
  var network = new Network()

  def receive = {
    case Initialize(muc) => room = muc
    case UserMessage(jid, message) => {
      if (message == "$say") {
        val phrase = network.doGenerate()
        sender ! SendMessage(room, phrase)
      } else {
        network.addPhrase(message)
      }
    }
  }
}
