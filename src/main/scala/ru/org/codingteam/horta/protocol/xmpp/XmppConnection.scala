package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, ActorRef, Kill}
import org.jivesoftware.smack._
import org.jivesoftware.smack.packet.Message
import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.protocol.{GlobalUserId, RoomUserId}

private case class PrivateMessageReceived(time: DateTime, userId: GlobalUserId, text: String)
private case class RoomMessageReceived(time: DateTime, userId: RoomUserId, text: String)

private case class ConnectionParameters(server: String, login: String, password: String)

/**
 * XMPP connection actor.
 *
 * @param params connection parameters.
 */
private class XmppConnection(params: ConnectionParameters, parent: ActorRef) extends Actor with ActorLogging {

  private val connection = {
    val ConnectionParameters(server, login, password) = params
    log.info(s"Connecting to $server")

    val configuration = new ConnectionConfiguration(server)
    configuration.setReconnectionAllowed(false)

    val connection = new XMPPConnection(configuration)
    val chatManager = connection.getChatManager

    connection.addConnectionListener(connectionListener())
    chatManager.addChatListener(chatListener())

    connection.connect()
    connection.login(login, password)
    log.info(s"Connected to $server")

    connection
  }

  override def postStop(): Unit = {
    if (connection.isConnected) {
      connection.disconnect()
    }

    super.postStop()
  }

  override def receive: Receive = {
    // Internal messages:
    case m: PrivateMessageReceived => parent ! m
    case m: RoomMessageReceived => parent ! m
  }

  private def connectionListener(): ConnectionListener = new ConnectionListener() {
    override def reconnectionFailed(e: Exception): Unit = ()
    override def reconnectionSuccessful(): Unit = ()
    override def connectionClosedOnError(e: Exception): Unit = self ! Kill
    override def connectionClosed(): Unit = ()
    override def reconnectingIn(seconds: Int): Unit = ()
  }

  // TODO: Handle private messages in a specific way; check PrivateMucMessageHandler for it. ~ F
  private def chatListener(): ChatManagerListener = new ChatManagerListener {
    override def chatCreated(chat: Chat, createdLocally: Boolean): Unit = {
      chat.addMessageListener(new MessageListener {
        override def processMessage(chat: Chat, message: Message): Unit = {
          self ! PrivateMessageReceived(Clock.now, GlobalUserId(chat.getParticipant), message.getBody)
        }
      })
    }
  }
}
