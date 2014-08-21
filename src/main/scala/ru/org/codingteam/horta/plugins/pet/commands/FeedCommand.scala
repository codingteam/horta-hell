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

  val loosePTC = List(
    " вцепившись зубами в бедро и выдирая кусок ткани штанов с кошельком",
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
      val (coins, response) = if (pet.hunger < 20) {
        var c = pet.coins
        var r = "ERROR occurred"
        if (pet.hunger < 10) {
          if (pet.randomGen.nextInt(5) == 0) {
            c = PtcUtils.updatePTC(username, pet.coins, -5)
            r = s"${pet.nickname}" + pet.randomChoice(attackWhileFeeding) + username + pet.randomChoice(loosePTC) + s". Вы теряете 5PTC, зато ${pet.nickname} накормлен."
            }
          } else {
          c = PtcUtils.updatePTC(username, pet.coins, 1)
          r = s"${pet.nickname}" + pet.randomChoice(successfulFeeding) + ". Вы зарабатываете 1PTC."
          }
        (c, r)
        } else {
        (pet.coins, s"${pet.nickname}" + pet.randomChoice(dontWant) + ".")
        }
      (pet.copy(hunger = 100, coins = coins), response)
      } else {
        (pet, "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует.")
    }
  }
}
