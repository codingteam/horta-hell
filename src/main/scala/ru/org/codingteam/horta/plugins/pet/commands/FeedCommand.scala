package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, Pet}
import ru.org.codingteam.horta.security.Credential

class FeedCommand extends AbstractCommand {
  override def apply(pet: Pet, credential: Credential, args: Array[String]): (Pet, String) = {
    val username = credential.name

    if (pet.alive) {
      val (coins, response) = if (pet.hunger < 20) {
        (PtcUtils.updatePTC(username, pet.coins, 1), s"${pet.nickname} был близок к голодной смерти, но вы его вовремя покормили. Вы зарабатываете 1PTC.")
      } else {
        (pet.coins, s"${pet.nickname} покормлен.")
      }

      (pet.copy(hunger = 100, coins = coins), response)
    } else {
      (pet, "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует.")
    }
  }
}
