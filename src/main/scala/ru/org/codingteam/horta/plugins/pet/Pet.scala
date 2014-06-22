package ru.org.codingteam.horta.plugins.pet

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.database.{ReadObject, StoreObject}
import ru.org.codingteam.horta.plugins.pet.Pet.PetTick
import ru.org.codingteam.horta.plugins.pet.commands.AbstractCommand
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math._

class Pet(roomId: String, location: ActorRef) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(5 minutes)

  private val store = context.actorSelection("/user/core/store")
  private var petData: Option[PetData] = None
  
  override def preStart() {
    context.system.scheduler.schedule(15 seconds, 360 seconds, self, Pet.PetTick)
  } 

  override def receive = {
    case PetTick => processTick()
    case Pet.ExecuteCommand(command, invoker, arguments) => processCommand(command, invoker, arguments)
  }

  private def processTick() = processAction { pet =>
    val nickname = pet.nickname
    var alive = pet.alive
    var health = pet.health
    var hunger = pet.hunger
    var coins = pet.coins

    if (pet.alive) {
      health -= 1
      hunger -= 2

      if (hunger <= 0 || health <= 0) {
        alive = false
        coins = coins.mapValues(x => max(0, x - 1))
        sayToEveryone(location, s"$nickname умер в забвении. Все теряют по 1PTC.")
      } else if (hunger <= 10 && pet.hunger > 10) {
        sayToEveryone(location, s"$nickname пытается сожрать все, что найдет.")
      } else if (health <= 10 && pet.health > 10) {
        sayToEveryone(location, s"$nickname забился в самый темный угол конфы и смотрит больными глазами в одну точку.")
      }

      pet.copy(alive = alive, health = health, hunger = hunger, coins = coins)
    } else {
      pet
    }
  }

  private def processCommand(command: AbstractCommand, invoker: Credential, arguments: Array[String]) =
    processAction { pet =>
      val (newPet, response) = command(pet, invoker, arguments)
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
    val Some(_) = Await.result(store ? StoreObject("pet", Some(roomId), pet), 5 minutes)
  }

  private def readStoredData(): Option[PetData] = {
    val request = store ? ReadObject("pet", roomId)
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
