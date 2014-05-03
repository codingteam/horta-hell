package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef
import ru.org.codingteam.horta.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.actors.database.{StoreOkReply, StoreObject, ReadObject}
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.plugins.{PluginDefinition, CommandDefinition, CommandPlugin}
import scala.concurrent.Future

class PetPlugin extends CommandPlugin {

  case object PetTick

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  val store = context.actorSelection("/user/core/store")

  var pets = Map[String, Pet]()

  override def pluginDefinition = PluginDefinition(
    "pet",
    false,
    List(CommandDefinition(CommonAccess, "pet", null)),
    Some(new PetDAO()))

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

  override def processCommand(credential: Credential, token: Any, arguments: Array[String]) {
    credential.roomName match {
      case Some(room) =>
        val petF = pets.get(room) match {
          case Some(p) => Future.successful(p)
          case None => initializeRoom(room, credential.location)
        }

        for (pet <- petF) {
          val text = arguments match {
            case Array("help", _*) => help
            case Array("stats", _*) => stats(pet)
            case Array("kill", _*) => kill(room)
            case Array("resurrect", _*) => resurrect(room)
            case Array("feed", _*) => feed(room, credential.name)
            case Array("heal", _*) => heal(room, credential.name)
            case Array("change", "nick", newNickname, _*) => changeNickname(room, newNickname)
            case Array("coins", _*) => showCoins(room, credential.name)
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

  def feed(room: String, username: String) = {
    val pet = pets(room)
    if (pet.alive) {
      val gotPTC = pet.hunger < 20
      pets = pets.updated(room, pet.copy(hunger = 100))

      if (gotPTC) {
        earnPTC(room, username, 1)
        s"${pet.nickname} был близок к голодной смерти, но вы его вовремя покормили. Вы зарабатываете 1PTC."
      } else {
        s"${pet.nickname} покормлен."
      }
    } else {
      "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует."
    }
  }

  def heal(room: String, username: String) = {
    val pet = pets(room)
    if (pet.alive) {
      val gotPTC = pet.health < 20
      pets = pets.updated(room, pet.copy(health = 100))

      if (gotPTC) {
        earnPTC(room, username, 1)
        s"${pet.nickname} был совсем плох и, скорее всего, умер если бы вы его вовремя не полечили. Вы зарабатываете 1PTC."
      } else {
        s"${pet.nickname} здоров."
      }
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

  def showCoins(room: String, username: String) = {
    val pet = pets(room)
    val ptc = pet.coins.getOrElse(username, 0)
    s"У тебя есть ${ptc}PTC"
  }

  def earnPTC(room: String, username: String, value: Int) = {
    val pet = pets(room)
    val coins = pet.coins.getOrElse(username, 0)
    pets = pets.updated(room, pet.copy(coins = pet.coins.updated(username, coins + value)))
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
}
