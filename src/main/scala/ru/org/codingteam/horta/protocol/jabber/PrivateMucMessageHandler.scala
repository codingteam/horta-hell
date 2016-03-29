package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.{SendPrivateResponse, SendResponse}
import ru.org.codingteam.horta.security.Credential

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class PrivateMucMessageHandler(muc: ActorRef, nick: String, implicit val executor: ExecutionContext) extends Actor with ActorLogging {
  implicit val timeout = Timeout(60 seconds)

  val core = context.actorSelection("/user/core")

  override def receive = LoggingReceive {
    case SendResponse(credential, text) => muc ! SendPrivateResponse(credential, text)
    case SendPrivateResponse(credential, text) => muc ! SendPrivateResponse(credential, text)
    case UserMessage(message) =>
      val jid = message.getFrom
      val text = message.getBody
      (muc ? GetCredential(jid)) onSuccess {
        case roomCred: Credential => core ! CoreMessage(Clock.now, roomCred.copy(location = self), text)
      }
  }
}
