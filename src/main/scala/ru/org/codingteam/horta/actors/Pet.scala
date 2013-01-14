package ru.org.codingteam.horta.actors

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ru.org.codingteam.horta.messages._

class Pet(val messenger : ActorRef, val room : String) extends Actor with ActorLogging {
  var nickname = "Наркоман"
  var alive = true
  var health = 100
  var hunger = 100
  
  def receive = {
    case PetCommand(command: Array[String]) => {
      command match {
        case Array("help", _*) => help
        case Array("stats", _*) => stats
        case Array("kill", _*) => kill
        case Array("resurrect", _*) => resurrect
        case Array("feed", _*) => feed
        case Array("heal", _*) => heal
        case _ => messenger ! SendMessage(room, "Попробуйте $pet help.")
      }
    }

    case PetTick => {
      // TODO: this message should be sent periodically to modify stats
      health -= 1
      hunger -= 2
    }
  }

  def help = messenger ! SendMessage(room, "Доступные команды: help, stats, kill, resurrect, feed, heal")

  def stats = if (alive) {
    val message = """
                     |Кличка: %s
                     |Здоровье: %d
                     |Голод: %d""".stripMargin.format(nickname, health, hunger)
    response(message)
  } else
    response("%s мертв. Какие еще статы?".format(nickname))
  
  def kill = if (alive) {
    alive = false
    response("Вы жестоко убили питомца этой конфы.")
  } else
    response("%s уже мертв. Но вам этого мало, да?".format(nickname))
  
  def resurrect = if (alive) {
    response("%s и так жив. Зачем его воскрешать?".format(nickname))
  } else {
    alive = true
    response("Вы воскресили питомца этой конфы! Это ли не чудо?!")
  }

  def feed = if (alive) {
    hunger = 100
    response("%s покормлен".format(nickname))
  } else
    response("Вы пихаете еду в рот мертвого питомца. Удивительно, но он никак не реагирует.")

  def heal = if (alive) {
    health = 100
    response("%s здоров".format(nickname))
  } else
    response("Невозможно вылечить мертвого питомца.")

  def response(message : String) = messenger ! SendMessage(room, message)
}
