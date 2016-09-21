package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import ru.org.codingteam.horta.configuration.Configuration

import scala.concurrent.duration._

/**
 * Actor to communicate through Extensible Messaging and Presence Protocol aka Jabber.
 */
class Xmpp extends Actor with ActorLogging {

  private val connectionParams = ConnectionParameters(Configuration.server, Configuration.login, Configuration.password)
  private val supervisor = BackoffSupervisor.props(
    Backoff.onStop(
      Props(classOf[XmppConnection], connectionParams).withDispatcher("pinned-dispatcher"),
      childName = "connection",
      minBackoff = 5.seconds,
      maxBackoff = 120.seconds,
      randomFactor = 0.05
    ))
  private val connection = context.actorOf(supervisor, "connection-supervisor")

  override def receive: Receive = {
    // TODO: Implement these; see XmppProtocolWrapper for implementation spec. ~ F
    case JoinRoom(room) => ???
    case GetParticipants() => ???
    case SendRoomMessage(roomId, text) => ???
    case SendPrivateRoomMessage(userId, text) => ???
    case SendDirectMessage(userId, text) => ???
  }
}
