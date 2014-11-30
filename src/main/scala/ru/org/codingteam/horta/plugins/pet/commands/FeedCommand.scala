package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class FeedCommand extends AbstractCommand {

  private val SATIATION_THRESHOLD = 20
  private val LOW_SATIATION_THRESHOLD = 10
  private val ATTACK_PROBAB = 2 // 2 is for 1/2
  private val FEEDING_AWARD = 1
  private val BINGO_AWARD = 3
  private val ATTACK_PENALTY = -1
  private val FULL_SATIATION = 100

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential

    val username = credential.name

    if (pet.alive) {
      val (feed, response) = if (pet.satiation < SATIATION_THRESHOLD) {
        if (pet.satiation < LOW_SATIATION_THRESHOLD) {
          if (pet.random.nextInt(ATTACK_PROBAB) == 0) {
            PtcUtils.tryUpdatePTC(coins, username, ATTACK_PENALTY, "pet attacked while feeding")
            (true,
              random("%s attacked %s while feeding").format(pet.nickname, username)
                + random(" taking some PTC.") + " "
                + localize("You've lost %dPTC, but %s is satiated.").format(FEEDING_AWARD, pet.nickname))
          } else {
            PtcUtils.tryUpdatePTC(coins, username, BINGO_AWARD, "bingo")
            (true,
              random("You're lucky!") + " "
                + localize("You gain %dPTC and %s is satiated.").format(BINGO_AWARD, pet.nickname))
          }
        } else {
          PtcUtils.tryUpdatePTC(coins, username, FEEDING_AWARD, "feed pet")
          (true,
            random("%s is fully satiated.").format(pet.nickname)
              + " " + localize("You got %dPTC.").format(FEEDING_AWARD))
        }
      } else {
        (false, random("%s does not want to eat.").format(pet.nickname))
      }
      (pet.copy(satiation = if (feed) FULL_SATIATION else pet.satiation), response)
    } else {
      (pet, localize("You're putting the food to the death pet's month but it does not react."))
    }
  }

}
