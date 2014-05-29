package ru.org.codingteam.horta.plugins.log

import org.joda.time.DateTime

case class LogMessage(id: Option[Int], time: DateTime, room: String, sender: String, eventType: EventType, text: String)
