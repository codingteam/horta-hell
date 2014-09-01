package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class RatingCommand extends AbstractCommand {
  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    val coinData = PtcUtils.queryPTC(coins)
    val users = coinData.toStream.sortBy(-_._2).take(10).filter(_._2 > 0)

    val response = "\n" + users.map(user => {
      val name = user._1
      val amount = user._2
      s"$name: ${amount}PTC"
    }).mkString("\n")

    (pet, response)
  }
}
