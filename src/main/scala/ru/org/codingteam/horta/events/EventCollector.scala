package ru.org.codingteam.horta.events

import ru.org.codingteam.horta.messages.Event
import ru.org.codingteam.horta.plugins.BasePlugin

class EventCollector() extends BasePlugin {

  override def preStart(): Unit = {
    super.preStart()
    log.info("EventCollector started")
  }

  override def postStop(): Unit = {
    log.info("EventCollector stopped")
    super.postStop()
  }

  override protected def name: String = "eventCollector"

  def onEvent(event: Event) = ???
}
