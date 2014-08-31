package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object PtcUtils {

  implicit val timeout = Timeout(1.minute)

  def tryUpdatePTC(coins: ActorRef, username: String, delta: Int, transactionName: String): Boolean = {
    Await.result((coins ? UpdateUserPTC(transactionName, username, delta)).mapTo[Boolean], 1.minute)
  }

  def queryPTC(coins: ActorRef, user: String): Int = {
    val map = Await.result((coins ? GetPTC()).mapTo[Map[String, Int]], 1.minute)
    map.getOrElse(user, 0)
  }

}
