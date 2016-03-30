package ru.org.codingteam.horta.protocol.jabber

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.{Chat, ChatManagerListener, MessageListener}
import ru.org.codingteam.horta.messages.{ChatOpened, UserMessage}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

class ChatListener(val messenger: ActorRef,
                   implicit val executor: ExecutionContext) extends ChatManagerListener {
  implicit val timeout = Timeout(60 seconds)

  def chatCreated(chat: Chat, createdLocally: Boolean) {
    if (!createdLocally) {
      val future = messenger ? ChatOpened(chat) map {
        case Some(chatActor: ActorRef) => chat.addMessageListener(new ChatMessageListener(chatActor))
      }
      Await.ready(future, timeout.duration)
    }
  }
}

class ChatMessageListener(chatActor: ActorRef) extends MessageListener {
  override def processMessage(chat: Chat, message: Message): Unit = {
    chatActor ! UserMessage(message)
  }
}
