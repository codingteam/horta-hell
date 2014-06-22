package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class ChangeNickCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    args match {
      case Array(newNickname, _*) => {
        val coins = pet.coins
        val changer = credential.name

        if (PtcUtils.getPTC(changer, coins) >= 1) {
          val newPet = pet.copy(nickname = newNickname, coins = PtcUtils.updatePTC(changer, coins, -1))
          if (pet.alive) {
            (newPet, "Теперь нашего питомца зовут %s.".format(newNickname))
          } else {
            (newPet, "Выяснилось, что нашего питомца при жизни звали %s.".format(newNickname))
          }
        } else {
          (pet, "Недостаточно PTC.")
        }
      }

      case _ =>
        (pet, "Попробуй $pet help change-nick")
    }
  }
}
