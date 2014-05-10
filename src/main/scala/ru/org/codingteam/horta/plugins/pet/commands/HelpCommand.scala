package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.security.Credential
import ru.org.codingteam.horta.plugins.pet.Pet

class HelpCommand (availableCommandNames: List[String]) extends AbstractCommand {
  override def apply(pet: Pet, credential: Credential, args: Array[String]): (Pet, String) = {
    (pet, s"Доступные команды: ${availableCommandNames.mkString(", ")}")
  }
}
