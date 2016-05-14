package ru.org.codingteam.horta.test

import akka.pattern.ask
import ru.org.codingteam.horta.messages._

import scala.concurrent.Await

class EventSpec extends TestKitSpec {
  "Core" should {
    "allow plugins to register and unregister" in {
      core ! Unsubscribe(self)
      expectMsg(timeout.duration, PositiveReply)
      core ! Subscribe({ case _ => true }, self)
      expectMsg(timeout.duration, PositiveReply)
    }
    "dispatch external event to subscribed plugins" in {
      Await.ready({
        core ? Unsubscribe(self)
      }, timeout.duration)

      Await.ready({
        core ? Subscribe({ case _ => true }, self)
      }, timeout.duration)

      val event = new Event()
      core ! EventMessage(event)
      expectMsg(timeout.duration, EventMessage(event))
    }
  }
}
