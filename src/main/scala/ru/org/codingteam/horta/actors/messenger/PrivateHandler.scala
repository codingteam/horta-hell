package ru.org.codingteam.horta.actors.messenger

import akka.actor.{Actor, ActorLogging, ActorRef}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.User
import ru.org.codingteam.horta.messages.ProcessCommand
import ru.org.codingteam.horta.messages.UserMessage

class PrivateHandler(val messenger: ActorRef) extends Actor with ActorLogging {
	def receive() = {
		case UserMessage(message) => {
			val jid = message.getFrom
			val text = message.getBody

			messenger ! ProcessCommand(User.fromKnownJid(jid, self), text)
		}

		case GenerateCommand(jid, _, _) => {
			messenger ! SendChatMessage(jid, "Sorry, no.")
		}
	}
}
