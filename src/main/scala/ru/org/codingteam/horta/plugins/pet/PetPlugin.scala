package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.actors.database.StoreOkReply
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.plugins._
import scala.concurrent.Future
import scala.math._
import ru.org.codingteam.horta.plugins.pet.commands._
import ru.org.codingteam.horta.actors.database.StoreObject
import scala.Some
import ru.org.codingteam.horta.plugins.CommandDefinition
import ru.org.codingteam.horta.actors.database.ReadObject
import ru.org.codingteam.horta.messages.SendResponse

class PetPlugin extends BasePlugin with CommandProcessor {

  case object PetTick

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  val store = context.actorSelection("/user/core/store")

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

        val location = pet.location
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
          } else if (hunger <= 10) {
            sayToEveryone(location, s"$nickname пытается сожрать все, что найдет.")
          } else if (health <= 10) {
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
    credential.roomName match {
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

          credential.location ! SendResponse(credential, text)
        }

      case None =>
    }
  }

  // TODO: Call this method on entering the room. See issue #47 for details.
  def initializeRoom(roomName: String, room: ActorRef) = {
    (store ? ReadObject("pet", roomName)).map { response =>
      val pet = response match {
        case Some(PetStatus(nickname, alive, health, hunger, coins)) =>
          Pet(room, nickname, alive, health, hunger, coins)

        case None =>
          Pet.default(room)
      }

      pets += roomName -> pet
      pet
    }
  }

  def savePet(room: String) {
    val pet = pets(room)
    val state = PetStatus(pet.nickname, pet.alive, pet.health, pet.hunger, pet.coins)
    for (reply <- store ? StoreObject("pet", Some(room), state)) {
      reply match {
        case StoreOkReply =>
      }
    }
  }

  def sayToEveryone(location: ActorRef, text: String) {
    val credential = Credential.empty(location)
    location ! SendResponse(credential, text)
  }
}
