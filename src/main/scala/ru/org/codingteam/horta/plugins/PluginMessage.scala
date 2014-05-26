package ru.org.codingteam.horta.plugins

import akka.actor.ActorRef
import org.joda.time.DateTime
import ru.org.codingteam.horta.security.Credential

/**
 * Standard plugin messages that expected to be supported for any plugins.
 */
abstract sealed class PluginMessage

/**
 * This is the first message for plugin to respond. Must be responded with PluginDefinition.
 */
case object GetPluginDefinition extends PluginMessage

/**
 * A process command request.
 * @param user a user executing the command.
 * @param token token used when registering a command.
 * @param arguments command argument array.
 */
case class ProcessCommand(user: Credential, token: Any, arguments: Array[String])

/**
 * A process message request. It can be send to the plugin if it was registered for global message processing.
 * @param time time of an event.
 * @param user a user sending the message.
 * @param message a message sent.
 */
case class ProcessMessage(time: DateTime, user: Credential, message: String)

/**
 * A process room join request.
 * @param time time of an event.
 * @param roomJID JID of the room.
 * @param actor actor representing the room.
 */
case class ProcessRoomJoin(time: DateTime, roomJID: String, actor: ActorRef)

/**
 * A process room leave request.
 * @param time time of an event.
 * @param roomJID JID of the room.
 */
case class ProcessRoomLeave(time: DateTime, roomJID: String)

/**
 * A process participant join request.
 * @param time time of an event.
 * @param roomJID JID of the room.
 * @param participantJID JID of the joined participant.
 * @param actor actor representing the room.
 */
case class ProcessParticipantJoin(time: DateTime, roomJID: String, participantJID: String, actor: ActorRef)

/**
 * A process participant leave request.
 * @param time time of an event.
 * @param roomJID JID of the room.
 * @param participantJID JID of the left participant.
 * @param actor actor representing the room.
 */
case class ProcessParticipantLeave(time: DateTime, roomJID: String, participantJID: String, actor: ActorRef)