package ru.org.codingteam.horta.actors

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import platonus.{Filesystem, Network}
import org.jivesoftware.smackx.muc.MultiUserChat
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.{messages, Configuration}
import ru.org.codingteam.horta.messages.AddPhrase
import ru.org.codingteam.horta.messages.UserMessage
import ru.org.codingteam.horta.messages.SendMessage
import ru.org.codingteam.horta.security.User

class Room extends Actor with ActorLogging {
  var room: String = null
  var messenger: ActorRef = null
  var users = Map[String, ActorRef]()

  def receive = {
    case InitializeRoom(roomName, messengerRef) => {
      room = roomName
      messenger = messengerRef
    }

    case UserMessage(jid, message) => {
      val nick = nickByJid(jid)
      val user = userByNick(nick)
      user ! AddPhrase(message)
      messenger ! ProcessCommand(User.fromJid(jid, context.self), message)
    }

    case GenerateCommand(jid, command) => {
      val nick = nickByJid(jid)
      val user = userByNick(nick)
      if (command == "say" || command == "♥") {
        user ! GeneratePhrase(if (command != "♥") nick else "ForNeVeR")
      }
    }

    case ParsedPhrase(nick, message) => {
      val user = userByNick(nick)
      user ! AddPhrase(message)
    }

    case GeneratedPhrase(forNick, phrase) => {
      messenger ! SendMessage(room, prepareResponse(forNick, phrase))
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

  def userByNick(nick: String) = {
    val user = users.get(nick)
    user match {
      case Some(u) => u
      case None    => {
        val user = context.actorOf(Props[RoomUser])
        users = users.updated(nick, user)
        user
      }
    }
  }
}
