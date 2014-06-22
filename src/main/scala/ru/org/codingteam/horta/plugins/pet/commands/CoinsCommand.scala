package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class CoinsCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name
    val ptc = PtcUtils.getPTC(username, pet.coins)
    (pet, s"У тебя есть ${ptc}PTC")
  }
}
