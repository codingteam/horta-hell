package ru.org.codingteam.horta.test

import akka.actor.Props
import akka.pattern.ask
import ru.org.codingteam.horta.events.{EventCollector, EventEndpoint}
import ru.org.codingteam.horta.messages.{EventMessage, Subscribe, TwitterEvent}

class EventCollectorSpec extends TestKitSpec {

  class MockEndpoint extends EventEndpoint {

    override def start(): Unit = {}

    override def stop(): Unit = {}

    override def process(eventCollector: EventCollector): Unit = {
      eventCollector.onEvent(TwitterEvent("",""))
    }

    override def validate(): Boolean = { true }
  }

  "EventCollector" should {
    "relay event messages to core" in {
      core ? Subscribe({case _ => true}, self)
      val plugin = system.actorOf(Props(classOf[EventCollector], Seq(new MockEndpoint())))
      expectMsg(timeout.duration, EventMessage(TwitterEvent("","")))
    }
  }

}
