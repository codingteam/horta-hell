package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.security.Credential
import ru.org.codingteam.horta.plugins.pet.PetData

class HelpCommand (availableCommandNames: List[String]) extends AbstractCommand {

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    (pet, s"Доступные команды: ${availableCommandNames.mkString(", ")}")
  }

}
