package ru.org.codingteam.horta.plugins.pet

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.util.StringUtils
import ru.org.codingteam.horta.database.PersistentStore
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.messages.GetParticipants
import ru.org.codingteam.horta.plugins.pet.commands.AbstractCommand
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class Pet(roomId: String, location: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  import ru.org.codingteam.horta.plugins.pet.Pet._

  implicit val timeout = Timeout(5 minutes)

  private val store = context.actorSelection("/user/core/store")
  private var petData: Option[PetData] = None
  private var coins: ActorRef = _

  private val SATIATION_DECREASE = (1, 2)
  private val HEALTH_DECREASE = (0, 2)
  private val HUNGER_BOUNDS = (5, 12)
  private val HEALTH_BOUNDS = (9, 10)
  private val SPARSENESS_OF_EVENTS = 4 // 4 is for 1/4
  private val CHANCE_OF_ATTACK = 6 // 6 is for 1/6
  private val ATTACK_PENALTY = 3
  private val DEATH_PENALTY = 1
  private val FULL_SATIATION = 100

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

  private def processTick() = processAction { pet =>
    PtcUtils.queryPTCAsync(coins).map(_.keys).flatMap { coinHolders =>
      if (pet.alive) {
        val nickname = pet.nickname
        val health = pet.health - pet.randomInclusive(HEALTH_DECREASE)
        val satiation = pet.satiation - pet.randomInclusive(SATIATION_DECREASE)

        def credential = Credential.empty(location)
        if (satiation <= 0 || health <= 0) {
          credential.map { implicit c =>
            coins ! UpdateAllPTC("pet death", -DEATH_PENALTY)
            sayToEveryone(random("%s is dead.").format(nickname) + " " +
              localize("All members have lost %dPTC.").format(DEATH_PENALTY))
            pet.copy(alive = false, health = 0, satiation = 0)
          }
        } else if (satiation <= HUNGER_BOUNDS._2
          && satiation > HUNGER_BOUNDS._1
          && pet.random.nextInt(SPARSENESS_OF_EVENTS) == 0) {
          if (pet.random.nextInt(CHANCE_OF_ATTACK) == 0 && coinHolders.size > 0) {
            (location ? GetParticipants).mapTo[Protocol.ParticipantCollection].map { map =>
              val possibleVictims = map.keys map ((x: String) => StringUtils.parseResource(x))
              val victim = pet.randomChoice((coinHolders.toSet & possibleVictims.toSet).toList)
              coins ! UpdateUserPTCWithOverflow("pet aggressive attack", victim, -ATTACK_PENALTY)
              credential.map { implicit c =>
                sayToEveryone(random("%s aggressively attacked %s").format(nickname, victim)
                  + random(" due to hunger, taking some of his PTC.") + " "
                  + localize("%s loses %dPTC.").format(ATTACK_PENALTY))
                pet.copy(health = health, satiation = FULL_SATIATION)
              }
            }.flatMap(identity)
          } else {
            credential.map { implicit c =>
              sayToEveryone(random("%s is searching for food.").format(nickname))
              pet.copy(health = health, satiation = satiation)
            }
          }
        } else if (health <= HEALTH_BOUNDS._2 && pet.health > HEALTH_BOUNDS._1) {
          credential.map { implicit c =>
            sayToEveryone(random("%s's health is low.").format(nickname))
            pet.copy(health = health, satiation = satiation)
          }
        } else {
          Future.successful(pet)
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

  private def getPetData: Future[PetData] = (self ? GetPetDataInternal).mapTo[Option[PetData]].flatMap {
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

object Pet {

  case object PetTick
  case object GetPetDataInternal

  case class SetPetDataInternal(data: PetData)
  case class ExecuteCommand(command: AbstractCommand, invoker: Credential, arguments: Array[String])

}
