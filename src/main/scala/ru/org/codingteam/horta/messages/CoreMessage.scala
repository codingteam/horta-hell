package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import org.joda.time.DateTime
import ru.org.codingteam.horta.security.Credential

/**
 * Core system message.
 * @param time time of an event.
 * @param credential user credential.
 * @param text message text.
 */
case class CoreMessage(time: DateTime, credential: Credential, text: String)

/**
 * Core room join message.
 * @param time time of an event.
 * @param roomJID JID of the room.
 * @param roomActor actor representing the room.
 */
case class CoreRoomJoin(time: DateTime, roomJID: String, roomActor: ActorRef)

/**
 * Core room leave message.
 * @param time time of an event.
 * @param roomJID JID of the room.
 */
case class CoreRoomLeave(time: DateTime, roomJID: String)

/**
 * Core room topic changed message. Sent when entering the room or when admin changed the topic.
 * @param time the time of an event.
 * @param roomId room identifier.
 * @param text new topic text.
 * @param roomActor actor representing the room.
 */
case class CoreRoomTopicChanged(time: DateTime, roomId: String, text: String, roomActor: ActorRef)

/**
 * Core participant joined message.
 * @param time time of an event.
 * @param roomJID JID of the room participant joined in.
 * @param participantJID participant JID.
 * @param roomActor actor representing the room.
 */
case class CoreParticipantJoined(time: DateTime, roomJID: String, participantJID: String, roomActor: ActorRef)

/**
 * Core participant left message.
 * @param time time of an event.
 * @param roomJID JID of the room participant left from.
 * @param participantJID participant JID.
 * @param reason event reason (kicked, renamed, banned etc.).
 * @param roomActor actor representing the room.
 */
case class CoreParticipantLeft(time: DateTime,
                               roomJID: String,
                               participantJID: String,
                               reason: LeaveReason,
                               roomActor: ActorRef)
/**
 * Ask Core for a list of available commands
 */
case object CoreGetCommands