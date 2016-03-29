package ru.org.codingteam.horta.protocol.jabber

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.util.StringUtils
import org.jivesoftware.smack.{Chat, ChatManagerListener, MessageListener}
import ru.org.codingteam.horta.messages.{ChatOpened, ResolveJid, UserMessage}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class ChatListener(val messenger: ActorRef,
                   val privateHandler: ActorRef,
                   implicit val executor: ExecutionContext) extends ChatManagerListener {
  implicit val timeout = Timeout(60 seconds)

  def chatCreated(chat: Chat, createdLocally: Boolean) {
    if (!createdLocally) {
      messenger ? ChatOpened(chat)
      messenger ? ResolveJid(StringUtils.parseBareAddress(chat.getParticipant)) flatMap {
        case Some(roomActor: ActorRef) => roomActor ? ChatOpened(chat)
      } map {
        case Some(chatActor: ActorRef) => new ChatMessageListener(chatActor)
      } recover {
        case _ => new ChatMessageListener(privateHandler)
      } map chat.addMessageListener
    }
  }
}

class ChatMessageListener(chatActor: ActorRef) extends MessageListener {
  override def processMessage(chat: Chat, message: Message): Unit = {
    chatActor ! UserMessage(message)
  }
}
