package ru.org.codingteam.horta.protocol.jabber

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.StringUtils
import org.jivesoftware.smack.{Chat, ChatManagerListener, MessageListener}
import ru.org.codingteam.horta.messages.{UserMessage, ChatOpened, ResolveJid}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class ChatListener(val messenger: ActorRef,
                   val privateHandler: ActorRef,
                   implicit val executor: ExecutionContext) extends ChatManagerListener {
  implicit val timeout = Timeout(60 seconds)

  def chatCreated(chat: Chat, createdLocally: Boolean) {
    if (!createdLocally) {
      messenger ! ChatOpened(chat)
      chat.addMessageListener(new MessageListener {
        def processMessage(chat: Chat, message: Message) {
          messenger ? ResolveJid(StringUtils.parseBareAddress(chat.getParticipant)) map {
            case Some(actorRef: ActorRef) => actorRef
            case _ => privateHandler
          } recover {
            case _ => privateHandler
          } map {
            _ ! UserMessage(message)
          }
        }
      })
    }
  }
}
