package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PetData, PtcUtils}
import ru.org.codingteam.horta.security.Credential

trait PetCommand[TParam] extends AbstractCommand {

  protected def parseParams(params: Array[String]): Either[TParam, String]
  protected def price(param: TParam): Int
  protected def transactionName: String

  protected def onTransactionSuccess(pet: PetData, param: TParam): (PetData, String)
  protected def onTransactionFailure(pet: PetData, param: TParam): (PetData, String)

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    parseParams(args) match {
      case Left(param) => process(param, credential, coins, pet)
      case Right(error) => (pet, error)
    }
  }

  private def process(param: TParam, credential: Credential, coins: ActorRef, pet: PetData) = {
    val amount = price(param)
    val user = credential.name
    if (executeTransaction(coins, user, amount) != 0) {
      onTransactionSuccess(pet, param)
    } else {
      onTransactionFailure(pet, param)
    }
  }

  private def executeTransaction(coins: ActorRef, user: String, price: Int) =
    PtcUtils.tryUpdatePTC(coins, user, -price, transactionName)

}
