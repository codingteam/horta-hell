package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

object TransferAmountMatcher {
  def unapply(s: String): Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => Some(0)
  }
}

class TransferCommand extends AbstractCommand {
  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val sourceUser = credential.name
    args match {
      case Array(targetUser, TransferAmountMatcher(amount), _*) => {
        if (amount <= 0) {
          (pet, s"Некорректная сумма.")
        } else {
          if (PtcUtils.getPTC(sourceUser, pet.coins) < amount) {
            (pet, s"Недостаточно PTC.")
          } else {
            val newCoins = List(
              (sourceUser, -amount),
              (targetUser, amount)
            ).foldLeft(pet.coins) {
              case (coins, (username, amount)) => PtcUtils.updatePTC(username, coins, amount)
            }

            (pet.copy(coins = newCoins), s"Транзакция успешна.")
          }
        }
      }

      case _ =>
        (pet, "Попробуй $pet help transfer")
    }
  }
}
