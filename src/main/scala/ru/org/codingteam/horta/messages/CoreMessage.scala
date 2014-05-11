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
 * Request to send response to user.
 * @param credential user credential.
 * @param text response text.
 */
case class SendResponse(credential: Credential, text: String)

/**
 * Request to send response to user through the private message.
 * @param credential user credential.
 * @param text response text.
 */
case class SendPrivateResponse(credential: Credential, text: String)