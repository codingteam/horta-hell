package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class ChangeNickCommand extends AbstractCommand {
  private val charactersPerPetcoin = 10

  private def calcPriceOfNickname(nickName: String): Int = {
    (nickName.length + charactersPerPetcoin - 1) / charactersPerPetcoin
  }

  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    args match {
      case Array(newNickname, _*) => {
        val coins = pet.coins
        val changer = credential.name
        val price = calcPriceOfNickname(newNickname)

        if (PtcUtils.getPTC(changer, coins) >= price) {
          val newPet = pet.copy(nickname = newNickname, coins = PtcUtils.updatePTC(changer, coins, -price))
          if (pet.alive) {
            (newPet, "Теперь нашего питомца зовут %s.".format(newNickname))
          } else {
            (newPet, "Выяснилось, что нашего питомца при жизни звали %s.".format(newNickname))
          }
        } else {
          (pet, s"Недостаточно PTC. Требуется ${price}PTC за данную кличку.")
        }
      }

      case _ =>
        (pet, "Попробуй $pet help change-nick")
    }
  }
}
