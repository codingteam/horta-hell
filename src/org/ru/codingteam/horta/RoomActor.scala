package org.ru.codingteam.horta

import akka.actor.{Props, Actor, ActorLogging}
import messages.{Initialize, SendMessage, UserMessage}
import platonus.network
import org.jivesoftware.smackx.muc.MultiUserChat

class RoomActor extends Actor with ActorLogging {
  var room: MultiUserChat = null
  var networks = Map[String, network]()

  def receive = {
    case Initialize(muc) => room = muc
    case UserMessage(jid, message) => {
      if (message == "$say") {
        val network = networks.get(jid)
        if (network.isDefined) {
          val phrase = network.get.doGenerate()
          sender ! SendMessage(room, phrase)
        }
      } else {
        val network = if (networks.contains(jid)) {
          networks.get(jid).get
        } else {
          val n = new network()
          networks = networks.updated(jid, n)
          n
        }

        network.addPhrase(message)
      }
    }
  }
}
