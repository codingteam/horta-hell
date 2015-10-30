package ru.org.codingteam.horta.core

import org.joda.time.{Period, DateTime, DateTimeZone}

/**
 * Object for time manipulating.
 */
object Clock {

  def now = DateTime.now(DateTimeZone.UTC)

  def timeout(seconds: Int, prevTime: DateTime, currTime: DateTime): Boolean = {
    val period = new Period(prevTime, currTime)
    period.toStandardSeconds.getSeconds > seconds
  }
}
