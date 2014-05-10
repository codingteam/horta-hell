package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.Pet
import ru.org.codingteam.horta.security.Credential

class RatingCommand extends AbstractCommand {
  override def apply(pet: Pet, credential: Credential, args: Array[String]): (Pet, String) = {
    val coins = pet.coins
    val users = coins.toStream.sortBy(-_._2).take(10).filter(_._2 > 0)

    val response = "\n" + users.map(user => {
      val name = user._1
      val amount = user._2
      s"$name: ${amount}PTC"
    }).mkString("\n")

    (pet, response)
  }
}
