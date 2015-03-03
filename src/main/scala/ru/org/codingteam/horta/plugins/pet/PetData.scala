package ru.org.codingteam.horta.plugins.pet

import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.security.Credential
import scala.util.Random

case class PetData(nickname: String,
                   alive: Boolean,
                   health: Int,
                   satiation: Int,
                   birth: DateTime) {

  val random = new Random()

  def randomChoice(l: List[String]) = l(random.nextInt(l.length))

  def randomInclusive(bounds: (Int, Int)) = bounds._1 + random.nextInt(bounds._2 - bounds._1 + 1)

  def killed = copy(alive = false, health = 0, satiation = 0)
  def satiated = copy(satiation = Pet.FULL_SATIATION)

}

object PetData {

  def default(implicit credential: Credential) = PetData(Localization.localize("Junkie"), true, 100, 100, Clock.now)

}
