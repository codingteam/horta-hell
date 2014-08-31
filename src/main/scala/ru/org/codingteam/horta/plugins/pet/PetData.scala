package ru.org.codingteam.horta.plugins.pet

import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import java.util.Random

case class PetData(nickname: String,
                   alive: Boolean,
                   health: Int,
                   satiation: Int,
                   birth: DateTime) {
  val randomGen = new Random(System.currentTimeMillis())
  def randomChoice(l: List[String]) = {
    l(randomGen.nextInt(l.length))
  }
}

object PetData {

  def default = PetData("Наркоман", true, 100, 100, Clock.now)

}
