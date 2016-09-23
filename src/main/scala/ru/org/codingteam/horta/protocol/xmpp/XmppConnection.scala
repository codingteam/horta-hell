package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, ActorRef, Kill}
import org.jivesoftware.smack._
import org.jivesoftware.smack.packet.{Message, Packet}
import org.jivesoftware.smackx.muc.{MultiUserChat, ParticipantStatusListener}
import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.protocol.jabber.MucParticipantStatusListener
import ru.org.codingteam.horta.protocol.{GlobalUserId, RoomId, RoomUserId}

private case class ConnectRoom(room: RoomId, nickname: String, greeting: Option[String])

private case class ConnectionReady()
private case class PrivateMessageReceived(time: DateTime, userId: GlobalUserId, text: String)
private case class RoomMessageReceived(time: DateTime, userId: RoomUserId, text: String)

private case class ConnectionParameters(server: String, login: String, password: String)

/**
 * XMPP connection actor.
 *
 * @param params connection parameters.
 */
private class XmppConnection(params: ConnectionParameters, parent: ActorRef) extends Actor with ActorLogging {

  private val connectionListener: ConnectionListener = new ConnectionListener() {
    override def reconnectionFailed(e: Exception): Unit = ()
    override def reconnectionSuccessful(): Unit = ()
    override def connectionClosedOnError(e: Exception): Unit = self ! Kill
    override def connectionClosed(): Unit = ()
    override def reconnectingIn(seconds: Int): Unit = ()
  }

  // TODO: Handle private messages in a specific way; check PrivateMucMessageHandler for it. ~ F
  private val chatListener: ChatManagerListener = new ChatManagerListener {
    override def chatCreated(chat: Chat, createdLocally: Boolean): Unit = {
      chat.addMessageListener(new MessageListener {
        override def processMessage(chat: Chat, message: Message): Unit = {
          parent ! PrivateMessageReceived(Clock.now, GlobalUserId(chat.getParticipant), message.getBody)
        }
      })
    }
  }

  private val connection = {
    val ConnectionParameters(server, login, password) = params
    log.info(s"Connecting to $server")

    val configuration = new ConnectionConfiguration(server)
    configuration.setReconnectionAllowed(false)

    val connection = new XMPPConnection(configuration)
    val chatManager = connection.getChatManager

    connection.addConnectionListener(connectionListener)
    chatManager.addChatListener(chatListener)

    connection.connect()
    connection.login(login, password)
    log.info(s"Connected to $server")

    connection
  }

  /**
   * A set of the active room JIDs to avoid joining the same room multiple times.
   */
  private var activeRooms: Set[String] = Set()

  override def preStart(): Unit = {
    parent ! ConnectionReady()
  }

  override def postStop(): Unit = {
    if (connection.isConnected) {
      connection.disconnect()
    }

    super.postStop()
  }

  override def receive: Receive = {
    // Messages from Xmpp:
    case ConnectRoom(roomId, nickname, greeting) => joinRoom(roomId.id, nickname, greeting)
  }

  private def roomMessageListener(roomJid: String): PacketListener = new PacketListener {
    val roomId = RoomId(roomJid)

    override def processPacket(packet: Packet): Unit = {
      packet match {
        case message: Message => {
          // Little trick to ignore historical messages:
          val extension = message.getExtension("delay", "urn:xmpp:delay")
          if (extension == null) {
            val jid = message.getFrom
            val text = message.getBody
            parent ! RoomMessageReceived(Clock.now, RoomUserId(roomId, jid), text)
          }
        }
      }
    }
  }

  private def roomParticipantStatusListener(muc: MultiUserChat): ParticipantStatusListener =
    new MucParticipantStatusListener(muc, parent)

  private def joinRoom(roomJid: String, nickname: String, greeting: Option[String]): Unit = {
    if (!activeRooms.contains(roomJid)) {
      val muc = new MultiUserChat(connection, roomJid)
      muc.addMessageListener(roomMessageListener(roomJid))
      muc.addParticipantStatusListener(roomParticipantStatusListener(muc))

      muc.join(nickname)
      log.info(s"Joined room $roomJid")

      greeting match {
        case Some(text) => muc.sendMessage(text)
        case None =>
      }

      activeRooms += roomJid
    }
  }
}
