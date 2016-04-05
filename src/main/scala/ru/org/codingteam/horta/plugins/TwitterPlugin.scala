package ru.org.codingteam.horta.plugins

import akka.actor.ActorRef
import org.joda.time.DateTime
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.messages.{Event, TwitterEvent}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential

class TwitterPlugin extends EventProcessor with RoomProcessor {

  var rooms: Map[String,ActorRef] = Map()

  override val filter: PartialFunction[Event, Boolean] = {
    case TwitterEvent(_,_) => true
    case _ => false
  }

  /**
   * Process an event
   * @param event event to process
   */
  override protected def processEvent(event: Event): Unit = {
    case tweet: TwitterEvent => relayTweet(tweet)
  }

  def relayTweet(tweet: TwitterEvent): Unit = {
    rooms.values foreach { location =>
      Credential.empty(location) map { implicit credential =>
        val message = String.format(Localization.localize("@%s tweets: %s"), tweet.author, tweet.tweet)
        Protocol.sendResponse(location, credential, message)
      }
    }
  }

  /**
   * Plugin name.
   * @return unique plugin name.
   */
  override protected def name: String = "TwitterPlugin"

  /**
   * Process joining the room.
   * @param time time of an event.
   * @param roomJID JID of the room.
   * @param actor actor representing the room.
   */
  override protected def processRoomJoin(time: DateTime, roomJID: String, actor: ActorRef): Unit = {
    rooms = rooms.updated(roomJID, actor)
  }

  /**
   * Process leaving the room.
   * @param time time of an event.
   * @param roomJID JID of the room.
   */
  override protected def processRoomLeave(time: DateTime, roomJID: String): Unit = {
    rooms = rooms - roomJID
  }
}
