package ru.org.codingteam.horta.security

import akka.actor.ActorRef

/**
 * A user in command context.
 * @param location user location.
 * @param access user access level.
 * @param roomId identifier of user room if exist.
 * @param name some token local to location.
 * @param id unique protocol-dependent user id if available.  
 */
case class Credential(location: ActorRef,
                      access: AccessLevel,
                      roomId: Option[String],
                      name: String,
                      id: Option[Any])

object Credential {
  def empty(location: ActorRef) = Credential(location, CommonAccess, None, "", None)
  def forNick(location: ActorRef, nick: String) = Credential(location, CommonAccess, None, nick, None)
}