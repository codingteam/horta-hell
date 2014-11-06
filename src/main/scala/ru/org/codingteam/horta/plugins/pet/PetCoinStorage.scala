package ru.org.codingteam.horta.plugins.pet

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.database.{StoreObject, ReadObject}

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.math._

case class GetPTC()
case class UpdateUserPTC(transactionName: String, user: String, delta: Int)
case class UpdateUserPTCWithOverflow(transactionName: String, user: String, delta: Int)
case class UpdateAllPTC(transactionName: String, delta: Int)
case class TransferPTC(transactionName: String, sourceUser: String, targetUser: String, amount: Int)

class PetCoinStorage(room: String) extends Actor with ActorLogging {

  val waitFor = 1.minute
  implicit val timeout = Timeout(waitFor)

  val store = context.actorSelection("/user/core/store")

  var coins: Option[Map[String, Int]] = None

  override def receive = {
    case GetPTC() => withCoins("watch", c => { sender ! c; (Some(c), 0) })
    case UpdateUserPTC(t, user, delta) => sender ! withCoins(t, updatePetCoins(user, delta))
    case UpdateUserPTCWithOverflow(t, user, delta) => sender ! withCoins(t, updatePetCoinsWithOverflow(user, delta))
    case UpdateAllPTC(t, delta) => sender ! withCoins(t, updateAllPetCoins(delta))
    case TransferPTC(t, source, target, amount) => sender ! withCoins(t, transferPetCoins(source, target, amount))
  }

  private def withCoins(transactionName: String, action: Map[String, Int] => (Option[Map[String, Int]], Int)): Int = {
    val Some(oldCoins) = coins match {
      case Some(c) => Some(c)
      case None => Await.result(
        (store ? ReadObject(PetPlugin.name, PetCoinsId(room))).mapTo[Option[Map[String, Int]]],
        waitFor)
    }

    action(oldCoins) match {
      case (Some(rawNewCoins), result) => {
        val newCoins = rawNewCoins.filter(_._2 > 0)
        Await.result(
          store ? StoreObject(
            PetPlugin.name,
            Some(PetCoinsId(room)),
            PetCoinTransaction(transactionName, oldCoins, newCoins)),
          waitFor) match {
          case Some(_) => coins = Some(newCoins)
          case None => sys.error("Cannot process PTC transaction")
        }
        result
      }
      case (None, 0) => 0
      case other => sys.error(s"Logic error: $other")
    }
  }

  private def updatePetCoinsWithOverflow(user: String, delta: Int)
                                        (coins: Map[String, Int]): (Option[Map[String, Int]], Int) = {
    val balance = coins.get(user).getOrElse(0)
    val newBalance = balance + delta

    if (delta == 0) {
      (None, 0)
    } else {
      (Some(coins + (user -> newBalance)), if (newBalance < 0) -balance else delta)
    }
  }

  private def updatePetCoins(user: String, delta: Int)
                            (coins: Map[String, Int]): (Option[Map[String, Int]], Int) = {
    val balance = coins.get(user).getOrElse(0)
    val newBalance = balance + delta

    if (delta == 0 || newBalance < 0) {
      (None, 0)
    } else {
      (Some(coins + (user -> newBalance)), delta)
    }
  }

  private def updateAllPetCoins(delta: Int)(coins: Map[String, Int]): (Option[Map[String, Int]], Int) = {
    coins.keys.foldLeft((Some(coins), 0): (Option[Map[String, Int]], Int)) {
      case ((Some(c), diff), name) => updatePetCoins(name, delta)(c)
      case ((None, _), _) => sys.error("Impossible")
    }
  }

  private def transferPetCoins(source: String,
                               target: String,
                               amount: Int)(coins: Map[String, Int]): (Option[Map[String, Int]], Int) = {
    updatePetCoins(source, -amount)(coins) match {
      case (Some(coins), _) => updatePetCoins(target, amount)(coins)
      case (None, _) => (None, 0)
    }
  }

}
