package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class KillCommand extends AbstractCommand {

  val KILL_PRICE = 10

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential

    val username = credential.name
    val ptcChange = -KILL_PRICE
    val change = PtcUtils.tryUpdatePTC(coins, username, ptcChange, "kill pet", overflow = true)
    if (pet.alive) {
      if (change == ptcChange) {
        (pet.copy(alive = false),
          localize("You brutally killed this conference's pet. You've lost %dPTC for it.").format(KILL_PRICE))
      } else {
        (pet,
          localize("You haven't enough PTC for such a wacky action. You need no less than %dPTC. But we anyway will take anything from you.").format(KILL_PRICE))
      }
    } else {
      (pet, localize("%s is dead already, but that's not enough for you. We take %dPTC from you for even thinking about killing the pet.").format(pet.nickname, change))
    }
  }

}
