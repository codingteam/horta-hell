package org.ru.codingteam.horta

import akka.actor.{Actor, ActorLogging}
import messages.UserMessage

class RoomActor extends Actor with ActorLogging {
  def receive = {
    case UserMessage(jid, message) => log.info(s"Message from $jid: $message")
  }
}
