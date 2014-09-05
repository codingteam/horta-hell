package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class CoinsCommand extends AbstractCommand {

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name
    val ptc = PtcUtils.queryPTC(coins, username)
    (pet, s"У тебя есть ${ptc}PTC")
  }

}
