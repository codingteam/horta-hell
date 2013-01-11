package ru.org.codingteam.horta.actors

import akka.actor.{Props, ActorLogging, Actor}
import org.jivesoftware.smack.{PacketListener, XMPPConnection}
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smack.packet.{Message, Packet}
import ru.org.codingteam.horta.Configuration
import ru.org.codingteam.horta.messages.{SendMessage, UserMessage, Initialize, JoinRoom}

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

  def receive = {
    case JoinRoom(jid) => {
      log.info(s"JoinRoom($jid)")
      val actor = context.system.actorOf(Props[Room], name = jid)
      val muc = new MultiUserChat(connection, jid)
      actor ! Initialize(muc)

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
    }

    case SendMessage(muc, message) => muc.sendMessage(message)
  }
}
