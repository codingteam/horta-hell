package ru.org.codingteam.horta.events

import java.util.concurrent.ForkJoinPool

import ru.org.codingteam.horta.messages.{Event, EventMessage}
import ru.org.codingteam.horta.plugins.BasePlugin

import scala.concurrent.{ExecutionContext, Future}

class EventCollector(endpoints: Seq[EventEndpoint]) extends BasePlugin {

  implicit val executor = ExecutionContext.fromExecutorService(new ForkJoinPool(EventCollector.paralellism))

  override def preStart(): Unit = {
    super.preStart()
    endpoints foreach { ep =>
      ep.start()
      Future {
        ep.process(this)
      }
    }
    log.info("EventCollector started")
  }

  override def postStop(): Unit = {
    endpoints foreach {
      _.stop()
    }
    executor.shutdownNow() // TODO: do not headshot executor, shutdown it gracefully
    log.info("EventCollector stopped")
    super.postStop()
  }

  def onEvent(event: Event) = {
    core ! EventMessage(event)
  }

  override protected def name: String = "eventCollector"
}

object EventCollector {
  private val paralellism = 5
}
