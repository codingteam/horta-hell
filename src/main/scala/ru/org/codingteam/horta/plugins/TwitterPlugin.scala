package ru.org.codingteam.horta.plugins

import akka.actor.{ActorLogging, ActorRef}
import org.joda.time.DateTime
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.messages.{Event, TwitterEvent}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential

class TwitterPlugin extends EventProcessor with RoomProcessor with ActorLogging {

  override val filter: PartialFunction[Event, Boolean] = {
    case TwitterEvent(_, _) => true
    case _ => false
  }
  implicit val dispatcher = context.dispatcher
  var rooms: Map[String, ActorRef] = Map()

  /**
    * Process an event
    *
    * @param event event to process
    */
  override protected def processEvent(event: Event): Unit = {
    log.debug("Got event: {}", event)
    event match {
      case tweet: TwitterEvent => relayTweet(tweet)
      case _ => log.warning("Unexpected event: {}", event)
    }
  }

  def relayTweet(tweet: TwitterEvent): Unit = {
    log.info("Relaying {} to {}", tweet, rooms.keySet)
    rooms.values foreach { location =>
      Credential.empty(location) map { implicit credential =>
        val message = String.format(Localization.localize("@%s tweets: %s"), tweet.author, tweet.tweet)
        Protocol.sendResponse(location, credential, message)
        log.debug("Sent {} to {}", tweet, credential)
      }
    }
  }

  /**
    * Plugin name.
    *
    * @return unique plugin name.
    */
  override protected def name: String = "TwitterPlugin"

  /**
    * Process joining the room.
    *
    * @param time    time of an event.
    * @param roomJID JID of the room.
    * @param actor   actor representing the room.
    */
  override protected def processRoomJoin(time: DateTime, roomJID: String, actor: ActorRef): Unit = {
    Configuration.roomDescriptors find { rd => roomJID.equals(rd.room) } filter {
      _.isEventEnabled("twitter")
    } match {
      case Some(_) =>
        log.info("Adding {} to relay list", roomJID)
        rooms = rooms.updated(roomJID, actor)
      case None => log.info("Twitter events for {} are disabled.", roomJID)
    }
  }

  /**
    * Process leaving the room.
    *
    * @param time    time of an event.
    * @param roomJID JID of the room.
    */
  override protected def processRoomLeave(time: DateTime, roomJID: String): Unit = {
    rooms = rooms - roomJID
  }
}
