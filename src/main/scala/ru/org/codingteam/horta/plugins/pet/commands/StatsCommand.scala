package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import org.joda.time.{DateTime, Period}
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.PetData
import ru.org.codingteam.horta.security.Credential

class StatsCommand extends AbstractCommand {

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential

    val age = new Period(pet.birth, Clock.now).toDurationFrom(DateTime.now).toStandardHours.getHours
    val response = if (pet.alive) {
      localize("\nNickname: %s\nHealth: %d\nSatiation: %d\nAge: %d hours").format(
        pet.nickname,
        pet.health,
        pet.satiation,
        age)
    } else {
      localize("%s is dead; his stats are unknown.").format(pet.nickname)
    }

    (pet, response)
  }

}
