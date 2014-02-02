package ru.org.codingteam.horta.protocol.jabber

import akka.pattern.ask
import org.jivesoftware.smack.{MessageListener, Chat, ChatManagerListener}
import akka.actor.ActorRef
import org.jivesoftware.smack.packet.Message
import ru.org.codingteam.horta.messages.{ChatOpened, UserMessage}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import concurrent.ExecutionContext

class ChatListener(
					  val messenger: ActorRef,
					  val privateHandler: ActorRef,
					  implicit val executor: ExecutionContext) extends ChatManagerListener {
	implicit val timeout = Timeout(60 seconds)

	def chatCreated(chat: Chat, createdLocally: Boolean) {
		if (!createdLocally) {
			for (reply <- messenger.ask(ChatOpened(chat))) {
				chat.addMessageListener(new MessageListener {
					def processMessage(chat: Chat, message: Message) {
						privateHandler ! UserMessage(message)
					}
				})
			}
		}
	}
}
