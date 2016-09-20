package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, Props}
import ru.org.codingteam.horta.configuration.Configuration

/**
 * Actor to communicate through Extensible Messaging and Presence Protocol aka Jabber.
 */
class Xmpp extends Actor with ActorLogging {

  private val connectionParams = ConnectionParameters(Configuration.server, Configuration.login, Configuration.password)
  private val connection = context.actorOf(Props(classOf[XmppConnection], connectionParams), "connection")
  // TODO: Specify the restart strategy for the connection to restart it on any issues. ~ F

  override def preStart(): Unit = {
    super.preStart()
  }

  override def receive: Receive = {
    // TODO: Implement these; see XmppProtocolWrapper for implementation spec. ~ F
    case JoinRoom(room) => ???
    case GetParticipants() => ???
    case SendRoomMessage(roomId, text) => ???
    case SendPrivateRoomMessage(userId, text) => ???
    case SendDirectMessage(userId, text) => ???
  }
}
