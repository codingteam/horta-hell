package ru.org.codingteam.horta.actors

import akka.actor.{Props, ActorLogging, Actor}
import org.jivesoftware.smack.{PacketListener, XMPPConnection}
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smack.packet.{Message, Packet}
import ru.org.codingteam.horta.Configuration
import ru.org.codingteam.horta.messages._

class Messenger extends Actor with ActorLogging {
  lazy val connection = {
    val server = Configuration.server
    log.info(s"Connecting to $server")

    val connection = new XMPPConnection(server)
    connection.connect()
    connection.login(Configuration.login, Configuration.password)
    log.info("Login succeed")

    connection
  }

  var rooms = Map[String, MultiUserChat]()

  def receive = {
    case InitializePlugin(core, plugins) => {
      val self = context.self
      Configuration.rooms foreach { case (roomName, jid) => self ! JoinRoom(jid) }
    }

    case JoinRoom(jid) => {
      log.info(s"JoinRoom($jid)")
      val actor = context.system.actorOf(Props[Room])
      actor ! InitializeRoom(jid, context.self)
      val muc = new MultiUserChat(connection, jid)
      rooms = rooms.updated(jid, muc)

      muc.addMessageListener(new PacketListener {
        def processPacket(packet: Packet) {
          log.info(s"Packet received from $jid: $packet")
          packet match {
            case message: Message => actor ! UserMessage(message.getFrom, message.getBody)
          }
        }
      })

      muc.join("horta hell")
      muc.sendMessage("Muhahahaha!")

      val parser = context.actorOf(Props[LogParser])
      parser ! InitializeParser(jid, actor)
    }

    case SendMessage(room, message) => {
      val muc = rooms.get(room).get
      muc.sendMessage(message)
    }
  }
}
