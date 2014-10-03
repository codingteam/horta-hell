package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.{PtcUtils, PetData}
import ru.org.codingteam.horta.security.Credential

class FeedCommand extends AbstractCommand {

  private val successfulFeeding = List(
    " был близок к голодной смерти, но вы его вовремя покормили",
    " с отвращением давится, набивая желудок",
    " аккуратно придерживает передними лапками добычу, кушая",
    " с чавканьем грызёт еду",
    " вгрызается в пищу, разрывая зубами на части",
    ", покосившись, брезгливо жуёт подачку",
    " с жадным сопением рыком уминает всю пищу в один присест",
    " клацнул зубами прямо возле руки, рывком забирая еду"
  )

  private val attackWhileFeeding = List(
    " накинулся в голодной ярости на ",
    " клацая зубами, рывком наскочил на ",
    " с рыком набросился на "
  )

  private val bingoMessages = List(
    "Чудо свершилось! Друг был другом спасён!",
    "Весьма вовремя покормлен был пет ибо голодная смерть ожидала его.",
    "Жестом доброй воли спасена зверушка от голода снедающего."
  )

  private val losePTC = List(
    ", вцепившись зубами в ногу и выдирая кусок ткани штанов с кошельком",
    ", едва давая увернуться ценой потери выпавшего кошелька",
    ", сжирая одежду и кошелёк"
  )

  private val dontWant = List(
    " сыт",
    " не голоден",
    " не желает есть",
    " отвернулся, брезгуя",
    " презрительно фыркнул, отстранившись от пищи",
    " опрокинул миску с едой лапой",
    " скривился от попытки его пичкать едой"
  )

  private val SATIATION_THRESHOLD = 20
  private val LOW_SATIATION_THRESHOLD = 10
  private val ATTACK_PROBAB = 2 // 2 is for 1/2
  private val FEEDING_AWARD = 1
  private val BINGO_AWARD = 3
  private val ATTACK_PENALTY = -1
  private val FULL_SATIATION = 100

  override def apply(pet: PetData, coins: ActorRef, credential: Credential, args: Array[String]): (PetData, String) = {
    val username = credential.name

    if (pet.alive) {
      val (feed, response) = if (pet.satiation < SATIATION_THRESHOLD) {
        if (pet.satiation < LOW_SATIATION_THRESHOLD) {
          if (pet.randomGen.nextInt(ATTACK_PROBAB) == 0) {
            PtcUtils.tryUpdatePTC(coins, username, ATTACK_PENALTY, "pet attacked while feeding")
            (true,
              s"${pet.nickname}" + pet.randomChoice(attackWhileFeeding) + username + pet.randomChoice(losePTC) + s". Вы теряете ${FEEDING_AWARD}PTC, зато ${pet.nickname} накормлен.")
          } else {
            PtcUtils.tryUpdatePTC(coins, username, BINGO_AWARD, "bingo")
            (true,
              s"${pet.randomChoice(bingoMessages)} Вы получаете ${BINGO_AWARD}PTC, а ${pet.nickname} сыт и доволен.")
          }
        } else {
          PtcUtils.tryUpdatePTC(coins, username, FEEDING_AWARD, "feed pet")
          (true,
            s"${pet.nickname}" + pet.randomChoice(successfulFeeding) + s". Вы зарабатываете ${FEEDING_AWARD}PTC.")
        }
      } else {
        (false, s"${pet.nickname}" + pet.randomChoice(dontWant) + ".")
      }
      (pet.copy(satiation = if (feed) FULL_SATIATION else pet.satiation), response)
    } else {
      (pet, "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует.")
    }
  }

}
