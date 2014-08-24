package ru.org.codingteam.horta.plugins.pet.commands

import org.joda.time.Period
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.plugins.pet.PetData
import ru.org.codingteam.horta.security.Credential

class StatsCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val age = new Period(pet.birth, Clock.now)
    val response = if (pet.alive) {
      """
        |Кличка: %s
        |Здоровье: %d
        |Сытость: %d
        |Возраст: %d часов""".stripMargin.format(pet.nickname, pet.health, pet.satiation, age.toStandardHours.getHours)
    } else {
      s"%s мертв. Какие еще статы?".format(pet.nickname)
    }

    (pet, response)
  }
}
