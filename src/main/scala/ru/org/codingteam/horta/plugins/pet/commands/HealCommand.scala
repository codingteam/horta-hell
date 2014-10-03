package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class HealCommand extends AbstractCommand {

  private val LOWHEALTH = 20
  private val MAXHEALTH = 100
  private val HEALING_AWARD = 1

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {

    val username = credential.name

    if (pet.alive) {
      var newHealth = pet.health
      val response = if (pet.health < LOWHEALTH) {
        newHealth = MAXHEALTH
        PtcUtils.tryUpdatePTC(coins, username, HEALING_AWARD, "heal pet")
        s"${pet.nickname} был совсем плох и, скорее всего, умер бы, если бы вы его вовремя не полечили. Вы зарабатываете 1PTC."
      } else {
        s"${pet.nickname} здоров."
      }

      (pet.copy(health = newHealth), response)
    } else {
      (pet, "Невозможно вылечить мертвого питомца.")
    }
  }
}
