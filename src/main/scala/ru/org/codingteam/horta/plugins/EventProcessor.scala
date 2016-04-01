package ru.org.codingteam.horta.plugins

import akka.util.Timeout
import ru.org.codingteam.horta.messages.{Event, EventMessage, Subscribe, Unsubscribe}
import akka.pattern.ask
import scala.concurrent.duration._


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
    core ? Subscribe(filter, self)
  }

  override def postStop() = {
    core ? Unsubscribe(self)
  }

  override def receive = {
    case EventMessage(event) => processEvent(event)
    case other => super.receive(other)
  }
}
