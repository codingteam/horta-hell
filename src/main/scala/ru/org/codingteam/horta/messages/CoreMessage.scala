package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import ru.org.codingteam.horta.security.Credential

/**
 * Core system message.
 * @param credential user credential.
 * @param text message text.
 */
case class CoreMessage(credential: Credential, text: String)

/**
 * Core room join message.
 * @param roomJID JID of the room.
 * @param roomActor actor representing the room.
 */
case class CoreRoomJoin(roomJID: String, roomActor: ActorRef)

/**
 * Core room leave message.
 * @param roomJID JID of the room.
 */
case class CoreRoomLeave(roomJID: String)

/**
 * Core participant joined message.
 * @param roomJID JID of the room participant joined in.
 * @param participantJID participant JID.
 * @param roomActor actor representing the room.
 */
case class CoreParticipantJoined(roomJID: String, participantJID: String, roomActor: ActorRef)

/**
 * Core participant left message.
 * @param roomJID JID of the room participant left from.
 * @param participantJID participant JID.
 * @param roomActor actor representing the room.
 */
case class CoreParticipantLeft(roomJID: String, participantJID: String, roomActor: ActorRef)
