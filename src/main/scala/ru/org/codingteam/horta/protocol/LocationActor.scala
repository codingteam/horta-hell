package ru.org.codingteam.horta.protocol

import akka.actor.{ActorLogging, Actor}
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages.GetLocaleDefinition

/**
 * Actor representing user location.
 */
abstract class LocationActor(locale: LocaleDefinition) extends Actor with ActorLogging {
  def receive = {
    case GetLocaleDefinition() => sender ! locale
  }
}
