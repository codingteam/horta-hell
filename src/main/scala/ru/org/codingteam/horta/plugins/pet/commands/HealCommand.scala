package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class HealCommand extends AbstractCommand {
  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name

    if (pet.alive) {
      val response = if (pet.health < 20) {
        PtcUtils.tryUpdatePTC(coins, username, 1, "heal pet")
        s"${pet.nickname} был совсем плох и, скорее всего, умер бы, если бы вы его вовремя не полечили. Вы зарабатываете 1PTC."
      } else {
        s"${pet.nickname} здоров."
      }

      (pet.copy(health = 100), response)
    } else {
      (pet, "Невозможно вылечить мертвого питомца.")
    }
  }
}
