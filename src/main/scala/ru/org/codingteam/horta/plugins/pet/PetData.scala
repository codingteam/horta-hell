package ru.org.codingteam.horta.plugins.pet

case class PetData(nickname: String,
                   alive: Boolean,
                   health: Int,
                   hunger: Integer,
                   coins: Map[String, Int])

object PetData {
  val default = PetData("Наркоман", true, 100, 100, Map[String, Int]())
}
