package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, Pet}
import ru.org.codingteam.horta.security.Credential

class KillCommand extends AbstractCommand {
  override def apply(pet: Pet, credential: Credential, args: Array[String]): (Pet, String) = {
    val username = credential.name
    val userCoins = PtcUtils.getPTC(username, pet.coins)

    val newPet = pet.copy(
      coins = PtcUtils.updatePTC(username, pet.coins, -10),
      alive = pet.alive && userCoins < 10
    )

    val response = if (pet.alive) {
      if (userCoins < 10) {
        "У вас не достаточно PTC для совершения столь мерзкого поступка. Требуется не менее 10PTC. Но мы, все равно, их с вас снимаем"
      } else {
        "Вы жестоко убили питомца этой конфы. За это вы теряете 10PTC."
      }
    } else {
      s"${pet.nickname} уже мертв. Но вам этого мало, да? За одну лишь мысль об убийстве питомца с вас снимается 10PTC."
    }

    (newPet, response)
  }
}
