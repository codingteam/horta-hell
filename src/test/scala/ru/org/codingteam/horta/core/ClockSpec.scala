package ru.org.codingteam.horta.core

import org.joda.time.DateTime
import org.scalatest.{Matchers, FlatSpec}

class ClockSpec extends FlatSpec with Matchers {

  val base = new DateTime(2010, 1, 1, 0, 0)

  "Clock.timeout" should "be false when not timed out" in {
    val next = new DateTime(2010, 1, 1, 0, 0, 5)
    assert(!Clock.timeout(10, base, next))
  }

  it should "be true when timed out" in {
    val next = new DateTime(2010, 1, 1, 0, 0, 5)
    assert(Clock.timeout(1, base, next))
  }

  it should "work correctly with minutes" in {
    val next = new DateTime(2010, 1, 1, 0, 1, 5) // 01:05 overflow, would be problematic before fix of #344
    assert(Clock.timeout(10, base, next))
  }
}
