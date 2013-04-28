package ru.org.codingteam.horta.actors.messenger

import org.jivesoftware.smack.ConnectionListener
import akka.actor.ActorRef
import ru.org.codingteam.horta.messages.Reconnect

/**
 * XMPP connection listener.
 */
class XMPPConnectionListener(val messenger: ActorRef) extends ConnectionListener {
	def connectionClosed () {
		messenger ! Reconnect()
	}

	def connectionClosedOnError (e: Exception) {
		messenger ! Reconnect()
	}

	def reconnectingIn (seconds: Int) {}

	def reconnectionSuccessful () {}

	def reconnectionFailed (e: Exception) {}
}
