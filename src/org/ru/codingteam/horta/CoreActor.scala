package org.ru.codingteam.horta

import akka.actor.{ActorLogging, Actor}

class CoreActor extends Actor with ActorLogging {
  def receive = {
    case 0 => null
  }
}
