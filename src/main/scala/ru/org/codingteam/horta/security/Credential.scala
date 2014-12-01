package ru.org.codingteam.horta.security

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages.GetLocaleDefinition

import scala.concurrent.ExecutionContext

/**
 * A user in command context.
 * @param location user location.
 * @param locale locale user preference.
 * @param access user access level.
 * @param roomId identifier of user room if exist.
 * @param name some token local to location.
 * @param id unique protocol-dependent user id if available.  
 */
case class Credential(location: ActorRef,
                      locale: LocaleDefinition,
                      access: AccessLevel,
                      roomId: Option[String],
                      name: String,
                      id: Option[Any])

object Credential {

  def empty(location: ActorRef)(implicit executionContext: ExecutionContext, timeout: Timeout) =
    (location ? GetLocaleDefinition()).mapTo[LocaleDefinition].map { case locale =>
      Credential(location, locale, CommonAccess, None, "", None)
    }

  def forNick(location: ActorRef, nick: String)(implicit executionContext: ExecutionContext, timeout: Timeout) =
    (location ? GetLocaleDefinition()).mapTo[LocaleDefinition].map { case locale =>
      Credential(location, locale, CommonAccess, None, nick, None)
    }

}