package ru.org.codingteam.horta.core

import org.joda.time.{DateTime, DateTimeZone}

/**
 * Object for time manipulating.
 */
object Clock {

  def now = DateTime.now(DateTimeZone.UTC)

}
