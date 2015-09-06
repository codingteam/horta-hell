package ru.org.codingteam.horta.plugins.pet

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import ru.org.codingteam.horta.database.PersistentStore
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.pet.commands.AbstractCommand
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class Pet(roomId: String, location: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  import ru.org.codingteam.horta.plugins.pet.Pet._

  implicit val timeout = Timeout(5 minutes)

  private val store = context.actorSelection("/user/core/store")
  private var petData: Option[PetData] = None
  private var coins: ActorRef = _

  override def preStart() {
    context.system.scheduler.schedule(15 seconds, 6 minutes, self, Pet.PetTick)
    coins = context.actorOf(Props(new PetCoinStorage(roomId)))
  }

  override def receive = LoggingReceive {
    case PetTick => processTick()
    case Pet.ExecuteCommand(command, invoker, arguments) => processCommand(command, invoker, arguments)

    case SetPetDataInternal(data) => petData = Some(data)
    case GetPetDataInternal => sender ! petData
  }

  private def nextState(pet: PetData, coinHolders: Iterable[String]) = {
    val nickname = pet.nickname
    pet match {
      case DyingPet() =>
        PetTickResponse(pet.killed, takeDeathPenalty, { implicit c =>
          sayToEveryone(random("%s is dead.").format(nickname) + " " +
            localize("All members have lost %dPTC.").format(DEATH_PENALTY))
        })

      case HungryPet() if pet.random.nextInt(SPARSENESS_OF_EVENTS) == 0 =>
        if (pet.random.nextInt(CHANCE_OF_ATTACK) == 0 && coinHolders.size > 0) {
          val victim = getPetVictimAsync(pet, coinHolders)
          PetTickResponse(pet.satiated, attackVictim(context.dispatcher, victim), { implicit c =>
            victim.map { v =>
              sayToEveryone(random("%s aggressively attacked %s").format(nickname, v)
                + random(" due to hunger, taking some of his PTC.") + " "
                + localize("%s loses %dPTC.").format(nickname, ATTACK_PENALTY))
            }
          })
        } else {
          PetTickResponse(pet, noAction, { implicit c =>
            sayToEveryone(random("%s is searching for food.").format(nickname))
          })
        }

      case IllPet() =>
        PetTickResponse(pet, noAction, { implicit c =>
          sayToEveryone(random("%s's health is low.").format(nickname))
        })

      case _ => PetTickResponse(pet, noAction, { c => })
    }
  }

  private def getPetVictimAsync(pet: PetData, coinHolders: Iterable[String]) = {
    Protocol.getParticipants(location).map { map =>
      val possibleVictims = map.keys.map(StringUtils.parseResource)
      pet.randomChoice((coinHolders.toSet & possibleVictims.toSet).toList)
    }
  }

  private def processTick() = processAction { pet =>
    PtcUtils.queryPTCAsync(coins).map(_.keys).flatMap { coinHolders =>
      if (pet.alive) {
        val health = pet.health - pet.randomInclusive(HEALTH_DECREASE)
        val satiation = pet.satiation - pet.randomInclusive(SATIATION_DECREASE)

        val state = nextState(pet.copy(health = health, satiation = satiation), coinHolders)
        state.coinAction(coins).map { _ =>
          Credential.empty(location).map(state.credentialAction)
          state.pet
        }
      } else {
        Future.successful(pet)
      }
    }
  }

  private def processCommand(command: AbstractCommand, invoker: Credential, arguments: Array[String]) =
    processAction { pet =>
      val (newPet, response) = command(pet, coins, invoker, arguments)
      Protocol.sendResponse(location, invoker, response)
      Future.successful(newPet)
    }

  private def processAction(action: PetData => Future[PetData]) {
    getPetData.flatMap(action).map(setPetData)
  }

  private def getPetData(): Future[PetData] = {
    (self ? GetPetDataInternal).mapTo[Option[PetData]].flatMap {
      case Some(data) => Future.successful(data)
      case None =>
        readStoredData().flatMap {
          case Some(dbData) => Future.successful(dbData)
          case None => Credential.empty(location).map {
            implicit c => PetData.default
          }
        }.map(data => {
          self ! SetPetDataInternal(data)
          data
        })
    }
  }

  private def setPetData(pet: PetData) {
    Await.result(
      PersistentStore.execute[PetRepository, Unit](PetPlugin.name, store)(_.storePetData(roomId, pet)), 5 minutes)
    self ! SetPetDataInternal(pet)
  }

  private def readStoredData(): Future[Option[PetData]] = {
    PersistentStore.execute[PetRepository, Option[PetData]](PetPlugin.name, store)(_.readPetData(roomId))
  }

  def sayToEveryone(text: String)(implicit credential: Credential) {
    Protocol.sendResponse(location, credential, text)
  }

}

case class PetTickResponse(pet: PetData,
                           coinAction: ActorRef => Future[Any],
                           credentialAction: Credential => Unit)

object Pet {

  val SATIATION_DECREASE = (1, 2)
  val HEALTH_DECREASE = (0, 2)
  val HUNGER_BOUNDS = (5, 12)
  val HEALTH_BOUNDS = (9, 10)
  val SPARSENESS_OF_EVENTS = 4 // 4 is for 1/4
  val CHANCE_OF_ATTACK = 6 // 6 is for 1/6
  val ATTACK_PENALTY = 3
  val DEATH_PENALTY = 1
  val FULL_SATIATION = 100

  case object PetTick
  case object GetPetDataInternal

  case class SetPetDataInternal(data: PetData)
  case class ExecuteCommand(command: AbstractCommand, invoker: Credential, arguments: Array[String])

  private def noAction(coins: ActorRef) = Future.successful(())

  private def takeDeathPenalty(coins: ActorRef): Future[Any] = {
    implicit val timeout = Timeout(5 minutes)
    coins ? UpdateAllPTC("pet death", -DEATH_PENALTY)
  }

  private def attackVictim(ec: ExecutionContext, victim: Future[String])(coins: ActorRef): Future[Any] = {
    implicit val timeout = Timeout(5 minutes)
    implicit val e = ec
    (victim map { v =>
      coins ? UpdateUserPTCWithOverflow("pet aggressive attack", v, -ATTACK_PENALTY)
    }).flatMap(identity _)
  }

}
