package ru.org.codingteam.horta

import akka.actor.{Props, Actor, ActorLogging}
import messages.{UserMessage, Initialize, SendMessage}
import platonus.{Filesystem, Network}
import org.jivesoftware.smackx.muc.MultiUserChat

class RoomActor extends Actor with ActorLogging {
  var room: MultiUserChat = null
  var networks = Map[String, Network]()

  def receive = {
    case Initialize(muc) => room = muc
    case UserMessage(jid, message) => {
      val nick = nickByJid(jid)

      val networkOption = networks.get(nick)
      val network = {
        if (networkOption.isDefined) {
          networkOption.get
        } else {
          val network = networkByNick(nick)
          networks = networks.updated(nick, network)
          network
        }
      }

      if (message == "$say") {
        val phrase = network.doGenerate()
        sender ! SendMessage(room, phrase)
      } else {
        network.addPhrase(message)
      }
    }
  }

  def nickByJid(jid: String) = {
    val args = jid.split('/')
    if (args.length > 1) {
      args(1)
    } else {
      args(0)
    }
  }
  def networkByNick(nick: String) = {
    log.info(s"Parsing log directory for $nick user.")
    Filesystem.scanDirectory(
      nick,
      Configuration.logDirectory,
      Configuration.logEncoding)
  }
}
