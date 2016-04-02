package ru.org.codingteam.horta.plugins

import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.messages.{Event, EventMessage, Subscribe, Unsubscribe}

import scala.concurrent.duration._
import scala.languageFeature.postfixOps


/**
 * Trait for the plugins that need to react to certain external events
 */
abstract class EventProcessor extends BasePlugin {

  /**
   * filter function for the events
   */
  val filter: PartialFunction[Event, Boolean]

  /**
   * Process an event
   * @param event event to process
   */
  protected def processEvent(event: Event): Unit

  implicit val timeout = Timeout(60 seconds)

  override def preStart() = {
    super.preStart()
    core ? Subscribe(filter, self)
  }

  override def postStop() = {
    core ? Unsubscribe(self)
    super.postStop()
  }

  override def receive = {
    case EventMessage(event) => processEvent(event)
    case other => super.receive(other)
  }
}
