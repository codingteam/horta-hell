package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.Pet
import ru.org.codingteam.horta.security.Credential

class StatsCommand extends AbstractCommand {
  override def apply(pet: Pet, credential: Credential, args: Array[String]): (Pet, String) = {
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
