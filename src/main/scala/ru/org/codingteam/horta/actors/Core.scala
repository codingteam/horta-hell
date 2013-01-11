package ru.org.codingteam.horta.actors

import akka.actor.{Props, ActorLogging, Actor}
import ru.org.codingteam.horta.messages.InitializePlugin

class Core extends Actor with ActorLogging {
  val plugins = {
    val messenger = context.actorOf(Props[Messenger](), "messenger")
    Map("messenger" -> messenger)
  }

  for (plugin <- plugins.values) {
    plugin ! InitializePlugin(context.self, plugins)
  }

  def receive = {
    case AnyRef => {

    }
    // TODO: Process some plugin messages
  }
}
