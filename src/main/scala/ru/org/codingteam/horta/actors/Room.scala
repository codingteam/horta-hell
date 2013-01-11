package ru.org.codingteam.horta.actors

import akka.actor.{Props, Actor, ActorLogging}
import platonus.{Filesystem, Network}
import org.jivesoftware.smackx.muc.MultiUserChat
import ru.org.codingteam.horta.messages.{SendMessage, UserMessage, Initialize}
import ru.org.codingteam.horta.Configuration

class Room extends Actor with ActorLogging {
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
        sender ! SendMessage(room, prepareResponse(nick, phrase))
      } else {
        network.addPhrase(message)
      }

      if (message == "/♥/") {
        val network = networks.get("ForNeVeR")
        if (network.isDefined) {
          val phrase = network.get.doGenerate()
          sender ! SendMessage(room, prepareResponse(nick, phrase))
        }
      }
    }
  }

  def prepareResponse(nick: String, message: String) = s"$nick: $message"

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
