package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.plugins.pet.PtcUtils
import ru.org.codingteam.horta.security.Credential

class CoinsCommand extends PetCommand[Unit] {

  override protected def parseArguments(arguments: Array[String])(implicit credential: Credential) = Left(())

  override protected def price(param: Unit): Int = 0

  override protected def transactionName: String = sys.error("Not supported")

  override protected def onTransactionSuccess(context: PetCommandContext, param: Unit)
                                             (implicit credential: Credential) = {
    val username = credential.name
    val ptc = PtcUtils.queryPTC(context.coins, username)
    (context.pet, Localization.localize("You have %dPTC.")(credential).format(ptc))
  }

  override protected def onTransactionFailure(context: PetCommandContext, param: Unit)
                                             (implicit credential: Credential) = sys.error("Not supported")

}
