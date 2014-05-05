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
import scala.math._

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
            case Array("help", _*) => help
            case Array("stats", _*) => stats(pet)
            case Array("kill", _*) => kill(room, credential.name)
            case Array("resurrect", _*) => resurrect(room, credential.name)
            case Array("feed", _*) => feed(room, credential.name)
            case Array("heal", _*) => heal(room, credential.name)
            case Array("change", "nick", newNickname, _*) => changeNickname(room, newNickname)
            case Array("coins", _*) => showCoins(room, credential.name)
            case Array("transfer", targetName, amount) => transfer(room, credential.name, targetName, amount)
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

  def help = "Доступные команды: help, stats, kill, resurrect, feed, heal, change nick, coins, transfer"

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

  def kill(room: String, username: String) = {
    val pet = pets(room)
    val userCoins = getPTC(username, pet.coins)

    pets = pets.updated(room, pet.copy(
      coins = updatePTC(username, pet.coins, -10),
      alive = pet.alive && userCoins < 10
    ))

    if (pet.alive) {
      if (userCoins < 10) {
        "У вас не достаточно PTC для совершения столь мерзкого поступка. Требуется не менее 10PTC. Но мы, все равно, их с вас снимаем"
      } else {
        "Вы жестоко убили питомца этой конфы. За это вы теряете 10PTC."
      }
    } else {
      s"${pet.nickname} уже мертв. Но вам этого мало, да? За одну лишь мысль об убийстве питомца с вас снимается 10PTC."
    }
  }

  def resurrect(room: String, username: String) = {
    val pet = pets(room)
    val userCoins = getPTC(username, pet.coins)

    if (pet.alive) {
      s"${pet.nickname} и так жив. Зачем его воскрешать?"
    } else {
      pets = pets.updated(room, pet.copy(
        health = 100,
        hunger = 100,
        alive = true,
        coins = updatePTC(username, pet.coins, 3)
      ))
      "Вы воскресили питомца этой конфы! Это ли не чудо?! За это вы получаете 3PTC."
    }
  }

  def feed(room: String, username: String) = {
    val pet = pets(room)
    if (pet.alive) {
      val (coins, response) = if (pet.hunger < 20) {
        (updatePTC(username, pet.coins, 1), s"${pet.nickname} был близок к голодной смерти, но вы его вовремя покормили. Вы зарабатываете 1PTC.")
      } else {
        (pet.coins, s"${pet.nickname} покормлен.")
      }

      pets = pets.updated(room, pet.copy(
        hunger = 100,
        coins = coins
      ))

      response
    } else {
      "Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует."
    }
  }

  def heal(room: String, username: String) = {
    val pet = pets(room)
    if (pet.alive) {
      val (coins, response) = if (pet.health < 20) {
        (updatePTC(username, pet.coins, 1), s"${pet.nickname} был совсем плох и, скорее всего, умер если бы вы его вовремя не полечили. Вы зарабатываете 1PTC.")
      } else {
        (pet.coins, s"${pet.nickname} здоров.")
      }

      pets = pets.updated(room, pet.copy(
        health = 100,
        coins = coins
      ))

      response
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

  def transfer(room: String,
               sourceUser: String,
               targetUser: String,
               amountString: String): String = {
    val amount = try {
      Integer.parseInt(amountString)
    } catch {
      case _: NumberFormatException => 0
    }

    if (amount <= 0) {
      return s"Некорректная сумма."
    }

    val pet = pets(room)
    var coins = pet.coins

    if (getPTC(sourceUser, coins) < amount) {
      return s"Недостаточно PTC."
    }

    coins = updatePTC(sourceUser, coins, -amount)
    coins = updatePTC(targetUser, coins, amount)

    pets = pets.updated(room, pet.copy(coins = coins))
    s"Транзакция успешна."
  }

  def updatePTC(username: String, coins: Map[String, Int], value: Int) = {
    val userCoins = coins.getOrElse(username, 0)
    coins.updated(username, max(0, userCoins + value))
  }

  def getPTC(username: String, coins: Map[String, Int]) = {
    coins.getOrElse(username, 0)
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
