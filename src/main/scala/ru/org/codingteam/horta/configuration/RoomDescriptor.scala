package ru.org.codingteam.horta.configuration

import ru.org.codingteam.horta.localization.LocaleDefinition

case class RoomDescriptor(id: String, room: String, locale: LocaleDefinition, nickname: String, message: String, events: String) {
  var eventMap = Map[String, Boolean]()
  def isEventEnabled(event: String): Boolean = {
    eventMap.get(event) match {
      case None =>
        val computed = events.contains(event)
        eventMap = eventMap.updated(event, computed)
        computed
      case Some(cached) => cached
    }
  }
}
