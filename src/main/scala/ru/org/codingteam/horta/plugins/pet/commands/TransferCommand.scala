package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

object TransferAmountMatcher {
  def unapply(s: String): Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _: java.lang.NumberFormatException => Some(0)
  }
}

class TransferCommand extends AbstractCommand {

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential

    val sourceUser = credential.name
    args match {
      case Array(targetUser, TransferAmountMatcher(amount), _*) => {
        if (amount <= 0) {
          (pet, localize("Amount is incorrect."))
        } else {
          if (PtcUtils.tryTransferPTC(coins, sourceUser, targetUser, amount, s"$sourceUser -> $targetUser") != 0) {
            (pet, localize("Transaction ok."))
          } else {
            (pet, localize("Insufficient PTC."))
          }
        }
      }

      case _ =>
        (pet, localize("Try $pet help transfer."))
    }
  }

}
