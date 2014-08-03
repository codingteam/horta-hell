package ru.org.codingteam.horta.plugins.pet

import org.joda.time.DateTime;

case class PetData(nickname: String,
                   alive: Boolean,
                   health: Int,
                   hunger: Int,
		   birth: DateTime,
                   coins: Map[String, Int])

object PetData {
  val default = PetData("Наркоман", true, 100, 100, new DateTime(), Map[String, Int]())
}
