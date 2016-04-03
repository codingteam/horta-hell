package ru.org.codingteam.horta.events

import ru.org.codingteam.horta.messages.{Event, EventMessage}
import ru.org.codingteam.horta.plugins.BasePlugin

class EventCollector(endpointFactories: Seq[EndpointFactory]) extends BasePlugin {

  val endpoints = endpointFactories map {
    _.construct(this)
  }

  override def preStart(): Unit = {
    super.preStart()
    endpoints foreach {
      _.start()
    }
    log.info("EventCollector started")
  }

  override def postStop(): Unit = {
    endpoints foreach {
      _.stop()
    }
    log.info("EventCollector stopped")
    super.postStop()
  }

  def onEvent(event: Event) = {
    core ! EventMessage(event)
  }

  override protected def name: String = "eventCollector"
}
