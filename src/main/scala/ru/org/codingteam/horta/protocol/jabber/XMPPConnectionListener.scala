package ru.org.codingteam.horta.protocol.jabber

import org.jivesoftware.smack.{XMPPConnection, ConnectionListener}
import akka.actor.ActorRef
import ru.org.codingteam.horta.messages.Reconnect

/**
 * XMPP connection listener.
 */
class XMPPConnectionListener(messenger: ActorRef, connection: XMPPConnection) extends ConnectionListener {

  def connectionClosed() {}

  def connectionClosedOnError(e: Exception) {
    messenger ! Reconnect(connection)
  }

  def reconnectingIn(seconds: Int) {}

  def reconnectionSuccessful() {}

  def reconnectionFailed(e: Exception) {}

}
