package ru.org.codingteam.horta.protocol.jabber

import org.scalatest.{FlatSpec, Matchers}

class MucMessageHandlerSpec extends FlatSpec with Matchers {

  "MucMessageHandler" should "replace nothing in a one-letter nick" in {
    assert(MucMessageHandler.getNickReplacement("a") === "a")
  }

  it should "replace a first vowel in a nicknames with vowels" in {
    assert(MucMessageHandler.getNickReplacement("vowel") === "v-wel")
    assert(MucMessageHandler.getNickReplacement("FRIEDRICH") === "FR-EDRICH")
  }

  it should "replace a second character if there is no vowels" in {
    assert(MucMessageHandler.getNickReplacement("т прнс") === "т-прнс")
  }

  it should "never replace first or last letter"  in {
    assert(MucMessageHandler.getNickReplacement("ass") === "a-s")
    assert(MucMessageHandler.getNickReplacement("ssa") === "s-a")
    assert(MucMessageHandler.getNickReplacement("uggur") === "ugg-r")
  }
}
