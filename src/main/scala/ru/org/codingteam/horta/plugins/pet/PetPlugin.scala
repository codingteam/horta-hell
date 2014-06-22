package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime
import ru.org.codingteam.horta.database.{ReadObject, StoreObject}
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.plugins.pet.commands._
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.math._
import scala.Some

case object PetTick

/**
 * Plugin for managing the so-called pet. Distinct pet belongs to every room.
 */
class PetPlugin extends BasePlugin with CommandProcessor with RoomProcessor {

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  var pets = Map[String, Pet]()

  val petCommands = {
    val withoutHelp = Map(
      "rating" -> new RatingCommand,
      "stats" -> new StatsCommand,
      "kill" -> new KillCommand,
      "resurrect" -> new ResurrectCommand,
      "feed" -> new FeedCommand,
      "heal" -> new HealCommand,
      "change-nick" -> new ChangeNickCommand,
      "coins" -> new CoinsCommand,
      "transfer" -> new TransferCommand
    )

    withoutHelp.updated("help", new HelpCommand(withoutHelp.keys.toList))
  }

  object PetCommandMatcher {
    def unapply(commandName: String): Option[AbstractCommand] =
      petCommands.get(commandName)
  }

  override def name = "pet"

  override def commands = List(CommandDefinition(CommonAccess, "pet", null))

  override def dao = Some(new PetDAO())

  override def preStart() = {
    context.system.scheduler.schedule(15 seconds, 360 seconds, self, PetTick)
  }

  override def receive = {
    case PetTick =>
      for (pair <- pets) {
        val roomName = pair._1
        val pet = pair._2

        val Some(location) = pet.location
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

          val newPet = pet.copy(alive = alive, health = health, hunger = hunger, coins = coins)
          pets = pets.updated(roomName, newPet)

          savePet(roomName) // TODO: move to another place
        }
      }

    case other => super.receive(other)
  }

  override def processCommand(credential: Credential, token: Any, arguments: Array[String]) {
    credential.roomId match {
      case Some(room) =>
        val petF = pets.get(room) match {
          case Some(p) => Future.successful(p)
          case None => initializeRoom(room, credential.location)
        }

        for (pet <- petF) {
          val text = arguments match {
            case Array(PetCommandMatcher(command), args@_*) => {
              val (newPet, response) = command(pet, credential, args.toArray)
              pets = pets.updated(room, newPet)
              response
            }

            case _ => "Попробуйте $pet help."
          }

          Protocol.sendResponse(credential.location, credential, text)
        }

      case None =>
    }
  }

  override def processRoomJoin(time: DateTime, roomJID: String, actor: ActorRef) {
    initializeRoom(roomJID, actor)
  }

  override def processRoomLeave(time: DateTime, roomJID: String) {
    savePet(roomJID)
    pets -= roomJID
  }

  private def initializeRoom(roomJID: String, actor: ActorRef) = {
    (store ? ReadObject("pet", roomJID)).map {
      response =>
        pets.get(roomJID) match {
          case Some(pet) =>
            // Defense from possible race conditions:
            pet

          case None =>
            val pet = response match {
              case Some(Pet(_, nickname, alive, health, hunger, coins)) =>
                Pet(Some(actor), nickname, alive, health, hunger, coins)

              case None =>
                Pet.default(actor)
            }

            pets += roomJID -> pet
            pet
        }
    }
  }

  def savePet(room: String) {
    val pet = pets(room)
    for (reply <- store ? StoreObject("pet", Some(room), pet)) {
      reply match {
        case Some(_) =>
      }
    }
  }

  def sayToEveryone(location: ActorRef, text: String) {
    val credential = Credential.empty(location)
    Protocol.sendResponse(location, credential, text)
  }

}
