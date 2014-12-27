package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.security.Credential

class ChangeNickCommand extends PetCommand[String] {

  override protected def parseArguments(arguments: Array[String])(implicit credential: Credential) = {
    arguments match {
      case Array("") => Right(localize("Empty string is not a valid pet nick."))
      case Array(newNickname) => Left(newNickname.trim)
      case _ => Right(localize("Try $pet help change-nick."))
    }
  }

  override protected def price(nickname: String) =
    (nickname.length + charactersPerPetcoin - 1) / charactersPerPetcoin

  override protected def transactionName = "change pet nick"

  override protected def onTransactionSuccess(context: PetCommandContext, newNickname: String)
                                             (implicit credential: Credential) = {
    val pet = context.pet
    val newPet = pet.copy(nickname = newNickname)
    if (pet.alive) {
      (newPet, localize("Now our pet's name is %s.").format(newNickname))
    } else {
      (newPet, localize("That's a surprise that our pet's name was %s when it was alive.").format(newNickname))
    }
  }

  override protected def onTransactionFailure(context: PetCommandContext, newNickname: String)
                                             (implicit credential: Credential) = {
    val amount = price(newNickname)
    (context.pet, localize("Insufficient PTC. You need %dPTC to set this nick.").format(amount))
  }

  private val charactersPerPetcoin = 10

}
