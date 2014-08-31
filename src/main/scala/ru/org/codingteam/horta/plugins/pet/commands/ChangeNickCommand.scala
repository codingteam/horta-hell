package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class ChangeNickCommand extends AbstractCommand {
  private val charactersPerPetcoin = 10

  private def calcPriceOfNickname(nickName: String): Int = {
    (nickName.length + charactersPerPetcoin - 1) / charactersPerPetcoin
  }

  private def changeNickname(pet: PetData, coins: ActorRef, credential: Credential, newNickname: String): (PetData, String) = {
    newNickname match {
      case "" => (pet, "Пустая строка в качестве клички неприемлема")
      case _ =>
        val changer = credential.name
        val price = calcPriceOfNickname(newNickname)

        if (PtcUtils.tryUpdatePTC(coins, changer, price, "change pet nick")) {
          val newPet = pet.copy(nickname = newNickname)
          if (pet.alive) {
            (newPet, "Теперь нашего питомца зовут %s.".format(newNickname))
          } else {
            (newPet, "Выяснилось, что нашего питомца при жизни звали %s.".format(newNickname))
          }
        } else {
          (pet, s"Недостаточно PTC. Требуется ${price}PTC за данную кличку.")
        }
    }
  }

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    args match {
      case Array(newNickname, _*) => changeNickname(pet, coins, credential, newNickname.trim)
      case _ => (pet, "Попробуй $pet help change-nick")
    }
  }
}
