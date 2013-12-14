package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef
import ru.org.codingteam.horta.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.actors.database.{RegisterStore, StoreOkReply, StoreObject, ReadObject}
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandPlugin}

class PetPlugin extends CommandPlugin {

  case object PetTick

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  val core = context.actorSelection("/user/core")
  val store = context.actorSelection("/user/store")

  var pets = Map[String, Pet]()

  override def commandDefinitions = List(CommandDefinition(CommonAccess, "pet", null))

  override def preStart() = {
    core ! RegisterStore("pet", new PetDAO())
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

        if (pet.alive) {
          health -= 1
          hunger -= 2

          if (hunger <= 0 || health <= 0) {
            alive = false

            val credential = Credential.empty(location)
            val text = s"$nickname умер в забвении"
            location ! SendResponse(credential, text)
          }

          val newPet = pet.copy(alive = alive, health = health, hunger = hunger)
          pets = pets.updated(roomName, newPet)

          savePet(roomName) // TODO: move to another place
        }
      }

    case other => super.receive(other)
  }

  override def processCommand(user: Credential, token: Any, arguments: Array[String]) {
    val response = user.roomName match {
      case Some(room) =>
        pets.get(room) match {
          case Some(pet) => {
            val response = arguments match {
              case Array("help", _*) => help
              case Array("stats", _*) => stats(pet)
              case Array("kill", _*) => kill(room)
              case Array("resurrect", _*) => resurrect(room)
              case Array("feed", _*) => feed(room)
              case Array("heal", _*) => heal(room)
              case Array("change", "nick", newNickname, _*) => changeNickname(room, newNickname)
              case _ => "Попробуйте $pet help."
            }

            Some(response)
          }

          case None => Some("Pet does not exist.")
        }

      case None => None
    }

    response foreach  { text =>
      user.location ! SendResponse(user, text)
    }
  }

  // TODO: Call this method on entering the room. See issue #47 for details.
  def initializeRoom(roomName: String, room: ActorRef) {
    var pet: Pet = null
    for (obj <- core ? ReadObject("pet", roomName)) {
      obj match {
        case Some(PetStatus(nickname, alive, health, hunger)) => {
          pet = Pet(room, nickname, alive, health, hunger)
        }

        case None =>
          pet = Pet.default(room)
      }
    }

    pets += roomName -> pet
  }

  def help = "Доступные команды: help, stats, kill, resurrect, feed, heal, change nick"

  def stats(pet: Pet) = {
    if (pet.alive) {
      """
        |Кличка: %s
        |Здоровье: %d
        |Голод: %d""".stripMargin.format(pet.nickname, pet.health, pet.hunger)
    } else {
      s"%s мертв. Какие еще статы?".format(pet.nickname)
    }
  }

  def kill(room: String) = {
    val pet = pets(room)
    if (pet.alive) {
      pets = pets.updated(room, pet.copy(alive = false))
      "Вы жестоко убили питомца этой конфы."
    } else {
      "%s уже мертв. Но вам этого мало, да?".format(pet.nickname)
    }
  }

  def resurrect(room: String) = {
    val pet = pets(room)
    if (pet.alive) {
      "%s и так жив. Зачем его воскрешать?".format(pet.nickname)
    } else {
      pets = pets.updated(room, Pet.default(pet.location).copy(nickname = pet.nickname))
      "Вы воскресили питомца этой конфы! Это ли не чудо?!"
    }
  }

  def feed(room: String) = {
    val pet = pets(room)
    if (pet.alive) {
      pets = pets.updated(room, pet.copy(hunger = 100))
      "%s покормлен".format(pet.nickname)
    } else {
      "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует."
    }
  }

  def heal(room: String) = {
    val pet = pets(room)
    if (pet.alive) {
      pets = pets.updated(room, pet.copy(health = 100))
      "%s здоров".format(pet.nickname)
    } else {
      "Невозможно вылечить мертвого питомца."
    }
  }

  def changeNickname(room: String, newNickname: String) = {
    val pet = pets(room)
    pets = pets.updated(room, pet.copy(nickname = newNickname))
    if (pet.alive) {
      "Теперь нашего питомца зовут %s.".format(newNickname)
    } else {
      "Выяснилось, что нашего питомца при жизни звали %s.".format(newNickname)
    }
  }

  def savePet(room: String) {
    val pet = pets(room)
    val state = PetStatus(pet.nickname, pet.alive, pet.health, pet.hunger)
    for (reply <- core ? StoreObject("pet", Some(room), state)) {
      reply match {
        case StoreOkReply =>
      }
    }
  }
}
