package ru.org.codingteam.horta.plugins.pet

import akka.actor.{Props, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import ru.org.codingteam.horta.database.{ReadObject, StoreObject}
import ru.org.codingteam.horta.plugins.pet.Pet.PetTick
import ru.org.codingteam.horta.plugins.pet.commands.AbstractCommand
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential
import ru.org.codingteam.horta.messages.GetParticipants

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class Pet(roomId: String, location: ActorRef) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(5 minutes)

  private val store = context.actorSelection("/user/core/store")
  private var petData: Option[PetData] = None
  private var coins: ActorRef = null

  private val aggressiveAttack = List(
    " яростно набрасывается на ",
    " накидывается на ",
    " прыгает, выпустив когти, на ",
    " с рыком впивается в бедро ",
    " опрокинул ",
    " повалил наземь ",
    " с силой врезался лбом в живот "
  )

  private val losePTC = List(
    " от голода, крепко вцепившись зубами и выдирая кусок ткани штанов с кошельком",
    " раздирая в клочья одежду от голода и давая едва увернуться ценой потери выпавшего кошелька",
    " от жуткого голода, сжирая одежду и кошелёк",
    " и полосонул когтями, чудом зацепившись за сумку с кошельком вместо живота",
    " с рыком раздирая одежду и пожирая ошмётки вместе с кошельком"
  )

  private val searchingForFood = List(
    " пытается сожрать все, что найдет",
    " рыщет в поисках пищи",
    " жалобно скулит и просит еды",
    " рычит от голода",
    " тихонько поскуливает от боли в пустом желудке",
    " скребёт пол в попытке найти пропитание",
    " переворачивает всё вверх дном в поисках еды",
    " ловит зубами блох, пытаясь ими наесться",
    " грызёт ножку стола, изображая вселенский голод",
    " демонстративно гремит миской, требовательно ворча",
    " плотоядно смотрит на окружающих, обнажив зубы",
    " старательно принюхивается, пытаясь уловить хоть какой-нибудь запах съестного",
    " плачет от голода, утирая слёзы хвостом"
  )

  private val becomeDead = List(
    " умер в забвении с гримасой страдания на морде",
    " корчится в муках и умирает",
    " агонизирует, сжимая зубы в предсмертных судорогах",
    " издал тихий рык и испустил дух"
  )

  private val lowHealth = List(
    " забился в самый темный угол конфы и смотрит больными глазами в одну точку",
    " лежит и еле дышит, хвостиком едва колышет",
    " жалобно поскуливает, волоча заднюю лапу",
    " завалился на бок и окинул замутнённым болью взором конфу",
    " едва дышит, издавая хриплые звуки и отхаркивая кровавую пену"
  )

  private val MAIN_SATIATION_DECREASE = 1
  private val MAIN_HP_DECREASE = 0
  private val ADDITIONAL_HP_DECREASE = 1
  private val ADDITIONAL_SATIATION_DECREASE = 1
  private val VARIANCE_OF_ADDITIONAL_VALS = 4
  private val HUNGER_BOUNDS = (5, 12)
  private val HEALTH_BOUNDS = (9, 10)
  private val SPARSENESS_OF_EVENTS = 3 // bigger is rarer
  private val CHANCE_OF_ATTACK = 6 // 6 is for 1/6
  private val ATTACK_PENALTY = 3
  private val DEATH_PENALTY = 1
  private val FULL_SATIATION = 100
  
  override def preStart() {
    context.system.scheduler.schedule(15 seconds, 360 seconds, self, Pet.PetTick)
    coins = context.actorOf(Props(new PetCoinStorage(roomId)))
  } 

  override def receive = {
    case PetTick => processTick()
    case Pet.ExecuteCommand(command, invoker, arguments) => processCommand(command, invoker, arguments)
  }

  private def processTick() = processAction { pet =>
    val nickname = pet.nickname
    var alive = pet.alive
    var health = pet.health
    var satiation = pet.satiation
    val coinHolders = Await.result((coins ? GetPTC()).mapTo[Map[String, Int]], 1.minute).keys

    if (pet.alive) {
      health -= (MAIN_SATIATION_DECREASE + (pet.randomGen.nextInt(VARIANCE_OF_ADDITIONAL_VALS + 1) + ADDITIONAL_SATIATION_DECREASE + 1) / (VARIANCE_OF_ADDITIONAL_VALS - 1))
      satiation -= (MAIN_HP_DECREASE + ADDITIONAL_HP_DECREASE + (pet.randomGen.nextInt(VARIANCE_OF_ADDITIONAL_VALS + 1) + ADDITIONAL_HP_DECREASE + 1) / (VARIANCE_OF_ADDITIONAL_VALS - 1))

      if (satiation <= 0 || health <= 0) {
        alive = false
        coins ! UpdateAllPTC("pet death", -DEATH_PENALTY)
        sayToEveryone(location, s"$nickname" + pet.randomChoice(becomeDead) + s". Все теряют по ${DEATH_PENALTY}PTC.")
      } else if (satiation <= HUNGER_BOUNDS._2 && satiation > HUNGER_BOUNDS._1 && satiation % SPARSENESS_OF_EVENTS == 0) { // 12, 9, 6
        if (pet.randomGen.nextInt(CHANCE_OF_ATTACK) == 0 && coinHolders.size > 0) {
          val map = Await.result((location ? GetParticipants()).mapTo[Map[String, Any]], 5.seconds)
          val possibleVictims = map.keys map ((x: String) => StringUtils.parseResource(x))
          val victim = pet.randomChoice((coinHolders.toSet & possibleVictims.toSet).toList)
          coins ! UpdateUserPTCWithOverflow("pet aggressive attack", victim, -ATTACK_PENALTY)
          sayToEveryone(location, s"$nickname" + pet.randomChoice(aggressiveAttack) + victim + pet.randomChoice(losePTC) + s". $victim теряет ${ATTACK_PENALTY}PTC.")
          satiation = FULL_SATIATION
        } else {
          sayToEveryone(location, s"$nickname" + pet.randomChoice(searchingForFood) + ".")
        }
      } else if (health <= HEALTH_BOUNDS._2 && pet.health > HEALTH_BOUNDS._1) {
        sayToEveryone(location, s"$nickname" + pet.randomChoice(lowHealth) + ".")
      }

      pet.copy(alive = alive, health = health, satiation = satiation)
    } else {
      pet
    }
  }

  private def processCommand(command: AbstractCommand, invoker: Credential, arguments: Array[String]) =
    processAction { pet =>
      val (newPet, response) = command(pet, coins, invoker, arguments)
      Protocol.sendResponse(location, invoker, response)
      newPet
    }

  private def processAction(action: PetData => PetData) {
    val pet = getPetData()
    val newPet = action(pet)
    setPetData(newPet)
  }
  
  private def getPetData(): PetData = {
    petData match {
      case Some(data) => data
      case None =>
        val data = readStoredData() match {
          case Some(dbData) => dbData
          case None => PetData.default
        }

        petData = Some(data)
        data
    }
  }

  private def setPetData(pet: PetData) {
    val Some(_) = Await.result(store ? StoreObject("pet", Some(PetDataId(roomId)), pet), 5 minutes)
    petData = Some(pet)
  }

  private def readStoredData(): Option[PetData] = {
    val request = store ? ReadObject("pet", PetDataId(roomId))
    Await.result(request, 5 minutes).asInstanceOf[Option[PetData]]
  }

  def sayToEveryone(location: ActorRef, text: String) {
    val credential = Credential.empty(location)
    Protocol.sendResponse(location, credential, text)
  }
}

object Pet {
  case object PetTick
  
  case class ExecuteCommand(command: AbstractCommand, invoker: Credential, arguments: Array[String])
}
