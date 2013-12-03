package ru.org.codingteam.horta.actors.messenger

import akka.actor.ActorRef
import org.jivesoftware.smack.PacketListener
import org.jivesoftware.smack.packet.{Packet, Presence}
import ru.org.codingteam.horta.messages.UserPresence

class MucParticipantListener(val roomActor: ActorRef) extends PacketListener {
	def processPacket(packet: Packet) {
		packet match {
			case presence: Presence => {
				roomActor ! UserPresence(presence.getFrom, presence.getType)
			}
		}
	}
}
