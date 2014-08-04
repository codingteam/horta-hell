package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.plugins.pet.{PetData, PtcUtils}
import ru.org.codingteam.horta.security.Credential

class ResurrectCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    if (pet.alive) {
      (pet, s"${pet.nickname} и так жив. Зачем его воскрешать?")
    } else {
      val username = credential.name
      val newPet = pet.copy(
        health = 100,
        hunger = 100,
        birth = Clock.now,
        alive = true,
        coins = PtcUtils.updatePTC(username, pet.coins, 3)
      )

      (newPet, "Вы воскресили питомца этой конфы! Это ли не чудо?! За это вы получаете 3PTC.")
    }
  }
}
