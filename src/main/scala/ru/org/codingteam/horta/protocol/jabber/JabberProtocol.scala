package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import org.jivesoftware.smack.{Chat, ConnectionConfiguration, XMPPConnection, XMPPException}
import org.jivesoftware.smack.filter.{AndFilter, FromContainsFilter, PacketTypeFilter}
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.muc.MultiUserChat
import ru.org.codingteam.horta.configuration._
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.{SendChatMessage, SendMucMessage, SendPrivateMessage}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Lock
import scala.language.postfixOps
import scala.Some

class JabberProtocol() extends Actor with ActorLogging {

  case class RoomDefinition(chat: MultiUserChat, actor: ActorRef)

  import context.dispatcher

  implicit val timeout = Timeout(1 minute)

  val lock = new Lock()

  val core = context.actorSelection("/user/core")

  var connection: XMPPConnection = null
  var chats = Map[String, Chat]()

  var privateHandler: ActorRef = null
  var rooms = Map[String, RoomDefinition]()

  override def preStart() {
    privateHandler = context.actorOf(Props(new PrivateMessageHandler(self)), "privateHandler")
    initializeConnection()
  }

  override def postStop() {
    disconnect()
  }

  def receive = {
    case Reconnect(closedConnection) if connection == closedConnection =>
      disconnect()
      context.children.foreach(context.stop)
      initializeConnection()

    case Reconnect(otherConnection) =>
      log.info(s"Ignored reconnect request from connection $otherConnection")

    case JoinRoom(jid, nickname, greeting) =>
      log.info(s"Joining room $jid")
      val actor = context.actorOf(Props(new MucMessageHandler(self, jid, nickname)), jid)

      val muc = new MultiUserChat(connection, jid)
      rooms = rooms.updated(jid, RoomDefinition(muc, actor))

      muc.addMessageListener(new MucMessageListener(jid, actor, log))
      muc.addParticipantStatusListener(new MucParticipantStatusListener(muc, actor))

      val filter = new AndFilter(new PacketTypeFilter(classOf[Message]), new FromContainsFilter(jid))
      connection.addPacketListener(
        new MessageAutoRepeater(context.system, self, context.system.scheduler, jid, context.dispatcher),
        filter)

      muc.join(nickname)
      muc.sendMessage(greeting)

    case ChatOpened(chat) => {
      chats = chats.updated(chat.getParticipant, chat)
      sender ! PositiveReply
    }

    case SendMucMessage(jid, message) =>
      val muc = rooms.get(jid)
      sender ! (muc match {
        case Some(muc) => sendMessage(message, muc.chat.sendMessage)
        case None => false
      })

    case SendPrivateMessage(roomJid, nick, message) =>
      val muc = rooms.get(roomJid)
      sender ! (muc match {
        case Some(muc) =>
          val jid = s"$roomJid/$nick"

          // TODO: This check is unreliable, implement something better. ~ ForNeVeR
          val occupants = muc.chat.getOccupants
          if (occupants.asScala.contains(jid)) {
            val chat = muc.chat.createPrivateChat(jid, null)
            sendMessage(message, chat.sendMessage)
          } else {
            false
          }
        case None =>
          false
      })

    case SendChatMessage(jid, message) =>
      val chat = chats.get(jid)
      sender ! (chat match {
        case Some(chat) => sendMessage(message, chat.sendMessage)
        case None => false
      })
  }

  private def initializeConnection() {
    connection = connect()
  }

  private def connect(): XMPPConnection = {
    val server = Configuration.server
    log.info(s"Connecting to $server")

    val configuration = new ConnectionConfiguration(server)
    configuration.setReconnectionAllowed(false)

    val connection = new XMPPConnection(configuration)
    val chatManager = connection.getChatManager

    try {
      connection.connect()
    } catch {
      case e: Throwable =>
        log.error(e, "Error while connecting")
        context.system.scheduler.scheduleOnce(10 seconds, self, Reconnect(connection))
        return connection
    }

    connection.addConnectionListener(new XMPPConnectionListener(self, connection))
    chatManager.addChatListener(new ChatListener(self, privateHandler, context.system.dispatcher))

    connection.login(Configuration.login, Configuration.password)
    log.info("Login succeed")

    Configuration.roomDescriptors foreach {
      case rd =>
        if (rd.room != null) self ! JoinRoom(rd.room, rd.nickname, rd.message)
        else log.warning(s"No JID given for room ${rd.id}")
    }

    connection
  }

  private def disconnect() {
    if (connection != null && connection.isConnected) {
      log.info("Disconnecting")
      connection.disconnect()
      log.info("Disconnected")
    }
  }

  private def sendMessage(message: String, action: String => Unit) = {
    val result = try {
      action(message)
      true
    } catch {
      case e: XMPPException =>
        log.warning(s"Message sending failed: $e")
        false
    }

    val deadline = ((message.length * 35) milliseconds).fromNow //TODO make multiplier configurable
    Thread.sleep(deadline.timeLeft.toMillis)

    result
  }

}
