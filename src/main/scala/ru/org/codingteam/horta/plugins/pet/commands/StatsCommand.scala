package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.PetData
import ru.org.codingteam.horta.security.Credential
import org.joda.time.{DateTime, Period}

class StatsCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val age = new Period(pet.birth, DateTime.now)
    val response = if (pet.alive) {
      """
        |Кличка: %s
        |Здоровье: %d
        |Голод: %d
	|Возраст: %d часов""".stripMargin.format(pet.nickname, pet.health, pet.hunger, age.getHours)
    } else {
      s"%s мертв. Какие еще статы?".format(pet.nickname)
    }

    (pet, response)
  }
}
