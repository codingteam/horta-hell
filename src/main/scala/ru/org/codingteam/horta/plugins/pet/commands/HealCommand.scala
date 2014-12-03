package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class HealCommand extends AbstractCommand {

  private val LOWHEALTH = 20
  private val MAXHEALTH = 100
  private val HEALING_AWARD = 1

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential
    val username = credential.name

    if (pet.alive) {
      var newHealth = pet.health
      val response = if (pet.health < LOWHEALTH) {
        newHealth = MAXHEALTH
        PtcUtils.tryUpdatePTC(coins, username, HEALING_AWARD, "heal pet")
        localize("%s's health was in poor condition but you healed it.").format(pet.nickname) + " " +
          localize("You got %dPTC.").format(HEALING_AWARD)
      } else {
        localize("%s has declined your medical help.").format(pet.nickname)
      }

      (pet.copy(health = newHealth), response)
    } else {
      (pet, localize("You cannot heal a dead pet."))
    }
  }
}
