package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.{PetData, PtcUtils}
import ru.org.codingteam.horta.security.Credential

class ResurrectCommand extends AbstractCommand {

  val RESURRECT_BOUNTY = 3

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential

    if (pet.alive) {
      (pet, localize("%s is alive already and doesn't need to be resurrected.").format(pet.nickname))
    } else {
      val username = credential.name
      PtcUtils.tryUpdatePTC(coins, username, RESURRECT_BOUNTY, "pet resurrect")

      val newPet = pet.copy(
        health = 100,
        satiation = 100,
        birth = Clock.now,
        alive = true
      )

      (newPet,
        localize("You've resurrected this conference's pet! It was a miracle! You gain %dPTC for this.")
          .format(RESURRECT_BOUNTY))
    }
  }

}
