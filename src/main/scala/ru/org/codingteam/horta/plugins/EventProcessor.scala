package ru.org.codingteam.horta.plugins

import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.messages.{Event, EventMessage, Subscribe, Unsubscribe}

import scala.concurrent.duration._
import scala.languageFeature.postfixOps
import scala.util.{Failure, Success}


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

  implicit val executor = context.dispatcher

  override def preStart() = {
    super.preStart()
    log.debug("Subscribing {} to external events", this)
    (core ? Subscribe(filter, self)) onComplete {
      case Success(_) => log.info("Subscribed {} to external events", this)
      case Failure(ex) => log.error("Failure while trying to subscribe {}", ex)
    }
  }

  override def postStop() = {
    log.debug("Unsubscribing {} from external events", this)
    (core ? Unsubscribe(self)) onComplete {
      case Success(_) => log.info("Unsubscribed {} from external events", this)
      case Failure(ex) => log.error("Failure while trying to unsubscribe {}", ex)
    }
    super.postStop()
  }

  override def receive = {
    case EventMessage(event) => processEvent(event)
    case other => super.receive(other)
  }
}
