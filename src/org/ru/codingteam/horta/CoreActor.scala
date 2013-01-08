package org.ru.codingteam.horta

import akka.actor.{ActorLogging, Actor}
import messages.JoinRoom

class CoreActor extends Actor with ActorLogging {
  def receive = {
    case JoinRoom(jid) =>
  }
}
