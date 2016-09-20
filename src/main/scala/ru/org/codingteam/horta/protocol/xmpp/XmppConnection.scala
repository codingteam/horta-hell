package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, Kill}
import org.jivesoftware.smack._

private case class ConnectionParameters(server: String, login: String, password: String)

private case class DisconnectedMessage()

/**
 * XMPP connection actor.
 *
 * @param params connection parameters.
 */
private class XmppConnection(params: ConnectionParameters) extends Actor with ActorLogging {

  // TODO: Pin this actor to the single thread using custom dispatcher. ~ F
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


  override def receive: Receive = ??? // TODO: Handle messages and send them to the parent.

  private def connectionListener(): ConnectionListener = new ConnectionListener() {
    override def reconnectionFailed(e: Exception): Unit = ()
    override def reconnectionSuccessful(): Unit = ()
    override def connectionClosedOnError(e: Exception): Unit = self ! Kill
    override def connectionClosed(): Unit = ()
    override def reconnectingIn(seconds: Int): Unit = ()
  }

  private def chatListener(): ChatManagerListener = new ChatManagerListener {
    override def chatCreated(chat: Chat, createdLocally: Boolean): Unit = ??? // TODO: Subscribe to chat messages ~ F
  }
}
