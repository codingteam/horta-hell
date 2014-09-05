package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object PtcUtils {

  implicit val timeout = Timeout(1.minute)

  def tryUpdatePTC(coins: ActorRef,
                   username: String,
                   delta: Int,
                   transactionName: String,
                   overflow: Boolean = false): Int = {
    val message = if (overflow) {
      UpdateUserPTCWithOverflow(transactionName, username, delta)
    } else {
      UpdateUserPTC(transactionName, username, delta)
    }

    Await.result((coins ? message).mapTo[Int], 1.minute)
  }

  def tryTransferPTC(coins: ActorRef,
                     sourceUserName: String,
                     targetUserName: String,
                     amount: Int,
                     transactionName: String): Int = {
    val message = TransferPTC(transactionName, sourceUserName, targetUserName, amount)
    Await.result((coins ? message).mapTo[Int], 1.minute)
  }

  def queryPTC(coins: ActorRef) = {
    Await.result((coins ? GetPTC()).mapTo[Map[String, Int]], 1.minute)
  }

  def queryPTC(coins: ActorRef, user: String): Int = {
    val map = queryPTC(coins)
    map.getOrElse(user, 0)
  }

}
