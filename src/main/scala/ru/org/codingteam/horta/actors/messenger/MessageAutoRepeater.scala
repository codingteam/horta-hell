package ru.org.codingteam.horta.actors.messenger

import org.jivesoftware.smack.PacketListener
import org.jivesoftware.smack.packet.{Message, Packet}
import ru.org.codingteam.horta.messages.SendMucMessage
import akka.actor.{ActorRef, Scheduler}
import scala.concurrent.duration._
import concurrent.ExecutionContext

class MessageAutoRepeater(
							 val messenger: ActorRef,
							 val scheduler: Scheduler,
							 val jid: String,
							 implicit val executor: ExecutionContext) extends PacketListener {
	def processPacket(packet: Packet) {
		packet match {
			case message: Message => {
				val error = packet.getError
				if (error != null && error.getCondition == "resource-constraint") {
					val body = message.getBody
					scheduler.scheduleOnce(1 second) {
						messenger ! SendMucMessage(jid, "[Re]" + body)
					}
				}
			}
		}
	}
}
