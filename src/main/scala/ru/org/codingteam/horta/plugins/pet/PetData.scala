package ru.org.codingteam.horta.plugins.pet

import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock

case class PetData(nickname: String,
                   alive: Boolean,
                   health: Int,
                   hunger: Int,
                   birth: DateTime,
                   coins: Map[String, Int])

object PetData {
  def default = PetData("Наркоман", true, 100, 100, Clock.now, Map[String, Int]())
}
