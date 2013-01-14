package ru.org.codingteam.horta.actors

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import org.jivesoftware.smack.{PacketListener, XMPPConnection}
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smack.packet.{Message, Packet}
import ru.org.codingteam.horta.Configuration
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.UnknownUser

class Messenger(val core: ActorRef) extends Actor with ActorLogging {
  var connection: XMPPConnection = null
  var parser: ActorRef = null

  override def preStart() = {
    val server = Configuration.server
    log.info(s"Connecting to $server")

    connection = new XMPPConnection(server)
    connection.connect()
    connection.login(Configuration.login, Configuration.password)
    log.info("Login succeed")

    Configuration.rooms foreach { case (roomName, jid) => self ! JoinRoom(jid) }
    core ! RegisterCommand("say", UnknownUser, self)
    core ! RegisterCommand("♥", UnknownUser, self)
    core ! RegisterCommand("mdiff", UnknownUser, self)

    parser = context.actorOf(Props[LogParser])
  }

  var rooms = Map[String, MultiUserChat]()

  def receive = {
    case ExecuteCommand(user, command, arguments) => {
      val location = user.location
      command match {
        case "say" | "♥" => location ! GenerateCommand(user.jid, command)
        case "mdiff"     => location ! DiffCommand(user.jid, arguments)
      }
    }

    case JoinRoom(jid) => {
      log.info(s"JoinRoom($jid)")

      val actor = context.system.actorOf(Props(new Room(self, parser, jid)), jid)
      val muc = new MultiUserChat(connection, jid)
      rooms = rooms.updated(jid, muc)

      muc.addMessageListener(new PacketListener {
        def processPacket(packet: Packet) {
          log.info(s"Packet received from $jid: $packet")
          packet match {
            case message: Message => {
              // Little trick to ignore historical messages:
              val extension = message.getExtension("delay", "urn:xmpp:delay")
              if (extension == null) {
                actor ! UserMessage(message.getFrom, message.getBody)
              }
            }
          }
        }
      })

      muc.join(Configuration.nickname)
      muc.sendMessage("Muhahahaha!")
    }

    case SendMessage(room, message) => {
      val muc = rooms.get(room)
      muc match {
        case Some(muc) => muc.sendMessage(message)
        case None =>
      }
    }

    case ProcessCommand(user, message) => {
      core ! ProcessCommand(user, message)
    }
  }
}
