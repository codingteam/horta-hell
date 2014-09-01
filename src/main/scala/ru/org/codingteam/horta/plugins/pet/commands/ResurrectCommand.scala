package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.plugins.pet.{PetData, PtcUtils}
import ru.org.codingteam.horta.security.Credential

class ResurrectCommand extends AbstractCommand {

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    if (pet.alive) {
      (pet, s"${pet.nickname} и так жив. Зачем его воскрешать?")
    } else {
      val username = credential.name
      val true = PtcUtils.tryUpdatePTC(coins, username, 3, "pet resurrect")

      val newPet = pet.copy(
        health = 100,
        satiation = 100,
        birth = Clock.now,
        alive = true
      )

      (newPet, "Вы воскресили питомца этой конфы! Это ли не чудо?! За это вы получаете 3PTC.")
    }
  }

}
