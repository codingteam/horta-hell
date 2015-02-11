package ru.org.codingteam.horta.protocol

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import ru.org.codingteam.horta.protocol.jabber.{Role, Affiliation}
import ru.org.codingteam.horta.security.Credential
import scala.concurrent.duration._
import scala.concurrent.Future

object Protocol {

  case class Participant(jid: String, affiliation: Affiliation, role: Role)
  type ParticipantCollection = Map[String, Participant]

  def sendResponse(actor: ActorRef, credential: Credential, text: String)
                  (implicit timeout: Timeout = Timeout(60.seconds)): Future[Boolean] = {
    sendAndWrap(actor, SendResponse(credential, text))
  }

  def sendPrivateResponse(actor: ActorRef, credential: Credential, text: String)
                         (implicit timeout: Timeout = Timeout(60.seconds)): Future[Boolean] = {
    sendAndWrap(actor, SendPrivateResponse(credential, text))
  }

  private def sendAndWrap(actor: ActorRef, message: ProtocolMessage)
                 (implicit timeout: Timeout): Future[Boolean] = {
    (actor ? message).mapTo[Boolean]
  }

}
