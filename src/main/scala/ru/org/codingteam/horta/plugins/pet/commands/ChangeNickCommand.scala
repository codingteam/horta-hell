package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PetData, PtcUtils}
import ru.org.codingteam.horta.security.Credential
import ru.org.codingteam.horta.localization.Localization._

class ChangeNickCommand extends AbstractCommand {
  private val charactersPerPetcoin = 10

  private def calcPriceOfNickname(nickName: String): Int = {
    (nickName.length + charactersPerPetcoin - 1) / charactersPerPetcoin
  }

  private def changeNickname(pet: PetData, coins: ActorRef, credential: Credential, newNickname: String): (PetData, String) = {
    implicit val c = credential
    newNickname match {
      case "" => (pet, localize("Empty string is not a valid pet nick."))
      case _ =>
        val changer = credential.name
        val price = calcPriceOfNickname(newNickname)

        if (PtcUtils.tryUpdatePTC(coins, changer, -price, "change pet nick") != 0) {
          val newPet = pet.copy(nickname = newNickname)
          if (pet.alive) {
            (newPet, localize("Now our pet's name is %s.").format(newNickname))
          } else {
            (newPet, localize("That's a surprise that our pet's name was %s when it was alive.").format(newNickname))
          }
        } else {
          (pet, localize("Insufficient PTC. You need %dPTC to set this nick.").format(price))
        }
    }
  }

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    args match {
      case Array(newNickname, _*) => changeNickname(pet, coins, credential, newNickname.trim)
      case _ => (pet, localize("Try $pet help change-nick.")(credential))
    }
  }
}
