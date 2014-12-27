package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PetData, PtcUtils}
import ru.org.codingteam.horta.security.Credential

case class PetCommandContext(coins: ActorRef, pet: PetData)

trait PetCommand[TParam] extends AbstractCommand {

  protected def parseArguments(arguments: Array[String])(implicit credential: Credential): Either[TParam, String]
  protected def price(param: TParam): Int
  protected def transactionName: String

  protected def onTransactionSuccess(context: PetCommandContext, param: TParam)
                                    (implicit credential: Credential): (PetData, String)
  protected def onTransactionFailure(context: PetCommandContext, param: TParam)
                                    (implicit credential: Credential): (PetData, String)

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    implicit val c = credential
    val context = PetCommandContext(coins, pet)
    parseArguments(args) match {
      case Left(param) => process(context, param)
      case Right(error) => (pet, error)
    }
  }

  private def process(context: PetCommandContext, param: TParam)(implicit credential: Credential) = {
    price(param) match {
      case 0 => onTransactionSuccess(context, param)
      case amount =>
        val user = credential.name
        if (executeTransaction(context.coins, user, amount) != 0) {
          onTransactionSuccess(context, param)
        } else {
          onTransactionFailure(context, param)
        }
    }
  }

  private def executeTransaction(coins: ActorRef, user: String, price: Int) =
    PtcUtils.tryUpdatePTC(coins, user, -price, transactionName)

}
