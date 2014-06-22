package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.PetData
import ru.org.codingteam.horta.security.Credential

class StatsCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val response = if (pet.alive) {
      """
        |Кличка: %s
        |Здоровье: %d
        |Голод: %d""".stripMargin.format(pet.nickname, pet.health, pet.hunger)
    } else {
      s"%s мертв. Какие еще статы?".format(pet.nickname)
    }

    (pet, response)
  }
}
