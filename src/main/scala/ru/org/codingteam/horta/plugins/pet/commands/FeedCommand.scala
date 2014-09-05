package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class FeedCommand extends AbstractCommand {

  val successfulFeeding = List(
    " был близок к голодной смерти, но вы его вовремя покормили",
    " с отвращением давится, набивая желудок",
    " аккуратно придерживает передними лапками добычу, кушая",
    " с чавканьем грызёт еду",
    " вгрызается в пищу, разрывая зубами на части",
    ", покосившись, брезгливо жуёт подачку",
    " с жадным сопением рыком уминает всю пищу в один присест",
    " клацнул зубами прямо возле руки, рывком забирая еду"
  )

  val attackWhileFeeding = List(
    " накинулся в голодной ярости на ",
    " клацая зубами, рывком наскочил на ",
    " с рыком набросился на "
  )

  val bingoMessages = List(
    "Свершилось чудо! Друг спас друга!"
  )

  val losePTC = List(
    ", вцепившись зубами в ногу и выдирая кусок ткани штанов с кошельком",
    ", едва давая увернуться ценой потери выпавшего кошелька",
    ", сжирая одежду и кошелёк"
  )

  val dontWant = List(
    " сыт",
    " не голоден",
    " не желает есть",
    " отвернулся, брезгуя",
    " презрительно фыркнул, отстранившись от пищи",
    " опрокинул миску с едой лапой",
    " скривился от попытки его пичкать едой"
  )

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name

    if (pet.alive) {
      val (feed, response) = if (pet.satiation < 20) {
        if (pet.satiation < 10) {
          if (pet.randomGen.nextInt(2) == 0) {
            PtcUtils.tryUpdatePTC(coins, username, -1, "pet attacked while feeding")
            (false,
              s"${pet.nickname}" + pet.randomChoice(attackWhileFeeding) + username + pet.randomChoice(losePTC) + s". Вы теряете 1PTC, зато ${pet.nickname} накормлен.")
          } else {
            PtcUtils.tryUpdatePTC(coins, username, 5, "bingo")
            (true,
              s"${pet.randomChoice(bingoMessages)} Вы получаете 5PTC, а ${pet.nickname} сыт и доволен.")
          }
        } else {
          PtcUtils.tryUpdatePTC(coins, username, 1, "feed pet")
          (true,
            s"${pet.nickname}" + pet.randomChoice(successfulFeeding) + ". Вы зарабатываете 1PTC.")
        }
      } else {
        (false, s"${pet.nickname}" + pet.randomChoice(dontWant) + ".")
      }
      (pet.copy(satiation = if (feed) 100 else pet.satiation), response)
    } else {
      (pet, "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует.")
    }
  }

}
