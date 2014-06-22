package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class HealCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name

    if (pet.alive) {
      val (coins, response) = if (pet.health < 20) {
        (PtcUtils.updatePTC(username, pet.coins, 1), s"${pet.nickname} был совсем плох и, скорее всего, умер бы, если бы вы его вовремя не полечили. Вы зарабатываете 1PTC.")
      } else {
        (pet.coins, s"${pet.nickname} здоров.")
      }

      (pet.copy(health = 100, coins = coins), response)
    } else {
      (pet, "Невозможно вылечить мертвого питомца.")
    }
  }
}
