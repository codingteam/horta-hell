package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class KillCommand extends AbstractCommand {

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name
    val ptcChange = -10
    val change = PtcUtils.tryUpdatePTC(coins, username, ptcChange, "kill pet", overflow = true)
    if (pet.alive) {
      if (change == ptcChange) {
        (pet.copy(alive = false), "Вы жестоко убили питомца этой конфы. За это вы теряете 10PTC.")
      } else {
        (pet, "У вас недостаточно PTC для совершения столь мерзкого поступка. Требуется не менее 10PTC. Но мы всё равно забираем у вас то, что можем.")
      }
    } else {
      (pet, s"${pet.nickname} уже мертв. Но вам этого мало, да? За одну лишь мысль об убийстве питомца с вас снимается ${change}PTC.")
    }
  }

}
