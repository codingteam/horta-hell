package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{ActorSystem, ActorRef, Scheduler}
import concurrent.ExecutionContext
import org.jivesoftware.smack.PacketListener
import org.jivesoftware.smack.packet.{Message, Packet}
import ru.org.codingteam.horta.protocol.SendMucMessage

class MessageAutoRepeater(val system: ActorSystem,
                          val messenger: ActorRef,
                          val scheduler: Scheduler,
                          val jid: String,
                          implicit val executor: ExecutionContext) extends PacketListener {

  val log = akka.event.Logging(system, messenger)

  def processPacket(packet: Packet) {
    packet match {
      case message: Message =>
        val error = packet.getError
        if (error != null && error.getCondition == "resource-constraint") {
          val body = message.getBody
          messenger ! SendMucMessage(jid, body)
          log.warning("I had to repeat message \"" + body + "\" because server hates me")
        }
    }
  }
}
