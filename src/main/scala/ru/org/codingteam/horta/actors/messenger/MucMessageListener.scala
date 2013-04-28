package ru.org.codingteam.horta.actors.messenger

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import org.jivesoftware.smack.PacketListener
import org.jivesoftware.smack.packet.{Message, Packet}
import ru.org.codingteam.horta.messages.UserMessage

class MucMessageListener(val jid: String, val roomActor: ActorRef, log: LoggingAdapter) extends PacketListener {
	def processPacket(packet: Packet) {
		packet match {
			case message: Message => {
				// Little trick to ignore historical messages:
				val extension = message.getExtension("delay", "urn:xmpp:delay")
				if (extension == null) {
					roomActor ! UserMessage(message)
				}
			}
		}
	}
}
