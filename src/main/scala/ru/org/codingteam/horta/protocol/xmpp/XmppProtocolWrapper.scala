package ru.org.codingteam.horta.protocol.xmpp

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages.JoinRoom
import ru.org.codingteam.horta.protocol._

import scala.concurrent.duration._
import scala.concurrent.Future

/**
 * Wrapper around XmppProtocol actor implementing IProtocol spec.
 */
class XmppProtocolWrapper(system: ActorSystem, actor: ActorRef) extends IProtocol {

  implicit val timeout = Timeout(5.minutes)

  override def dispose(): Unit = {
    system.stop(actor)
  }

  override def joinRoom(roomId: RoomId, locale: LocaleDefinition, nickname: String, greeting: Option[String]): Unit = {
    // TODO: Resend these messages on actor restarts? From the Xmpp actor himself or from here? ~ F
    actor ! JoinRoom(roomId.id, locale, nickname, greeting)
  }

  override def getParticipants(roomId: RoomId): Future[Map[String, Participant]] = {
    actor.ask(GetParticipants).mapTo[Map[String, Participant]]
  }

  override def sendRoomMessage(roomId: RoomId, message: String): Future[Unit] = {
    actor.ask(SendRoomMessage(roomId, message)).mapTo[Unit]
  }

  override def sendPrivateRoomMessage(userId: RoomUserId, message: String): Future[Unit] = {
    actor.ask(SendPrivateRoomMessage(userId, message)).mapTo[Unit]
  }

  override def sendDirectMessage(userId: GlobalUserId, message: String): Future[Unit] = {
    actor.ask(SendDirectMessage(userId, message)).mapTo[Unit]
  }
}
