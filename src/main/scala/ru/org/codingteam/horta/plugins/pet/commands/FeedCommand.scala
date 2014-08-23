package ru.org.codingteam.horta.plugins.pet.commands

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
    " покормлен",
    " не желает есть",
    " отвернулся, брезгуя",
    " презрительно фыркнул, отстранившись от пищи",
    " опрокинул миску с едой лапой",
    " скривился от попытки его пичкать едой"
  )

  override def apply(pet: PetData, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name

    if (pet.alive) {
      val (feed, coins, response) = if (pet.hunger < 20) {
        if (pet.hunger < 10) {
          if (pet.randomGen.nextInt(2) == 0) {
            (false,
              PtcUtils.updatePTC(username, pet.coins, -1),
              s"${pet.nickname}" + pet.randomChoice(attackWhileFeeding) + username + pet.randomChoice(losePTC) + s". Вы теряете 1PTC, зато ${pet.nickname} накормлен.")
          } else {
            (true,
              PtcUtils.updatePTC(username, pet.coins, 5),
              s"${pet.randomChoice(bingoMessages)} Вы получаете 5PTC, а ${pet.nickname} сыт и доволен.")
          }
        } else {
          (true,
            PtcUtils.updatePTC(username, pet.coins, 1),
            s"${pet.nickname}" + pet.randomChoice(successfulFeeding) + ". Вы зарабатываете 1PTC.")
        }
      } else {
        (true, pet.coins, s"${pet.nickname}" + pet.randomChoice(dontWant) + ".")
      }
      (pet.copy(hunger = if (feed) 100 else pet.hunger, coins = coins), response)
    } else {
      (pet, "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует.")
    }
  }
}
