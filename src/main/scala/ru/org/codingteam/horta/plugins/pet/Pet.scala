package ru.org.codingteam.horta.plugins.pet

import akka.actor.{Actor, ActorLogging, ActorRef}
import ru.org.codingteam.horta.messages._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.actors.database.{RegisterStore, StoreOkReply, StoreObject, ReadObject}
import ru.org.codingteam.horta.plugins.pet.PetStatus
import ru.org.codingteam.horta.security.CommonAccess
import ru.org.codingteam.horta.actors.pet.PetDAO

class Pet(val room: ActorRef, val roomName: String) extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	val core = context.actorSelection("/user/core")

	var nickname = "Наркоман"
	var alive = true
	var health = 100
	var hunger = 100

	override def preStart() = {
    // TODO: Extend CommandPlugin. Next code migrated from the old Messenger class and should be removed:
    core ! RegisterCommand(CommonAccess, "pet", self)
    core ! RegisterStore("pet", new PetDAO())

    context.system.scheduler.schedule(15 seconds, 360 seconds, self, PetTick)
		for (obj <- core ? ReadObject("pet", roomName)) {
			obj match {
				case Some(PetStatus(nickname, alive, health, hunger)) => {
					this.nickname = nickname
					this.alive = alive
					this.health = health
					this.hunger = hunger
				}

				case None =>
			}
		}
	}

	def receive = {
		case PetCommand(command: Array[String]) => {
			command match {
				case Array("help", _*) => help
				case Array("stats", _*) => stats
				case Array("kill", _*) => kill
				case Array("resurrect", _*) => resurrect
				case Array("feed", _*) => feed
				case Array("heal", _*) => heal
				case Array("change", "nick", newNickname, _*) => changeNickname(newNickname)
				case _ => response("Попробуйте $pet help.")
			}
		}

		case PetTick => if (alive) {
			health -= 1
			hunger -= 2

			if (hunger <= 0 || health <= 0) {
				alive = false
				response("%s умер в забвении".format(nickname))
			}

			savePet() // TODO: move to another place
		}
	}

	def help = response("Доступные команды: help, stats, kill, resurrect, feed, heal, change nick")

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
		health = 100
		hunger = 100
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

	def changeNickname(newNickname: String) = {
		nickname = newNickname
		if (alive)
			response("Теперь нашего питомца зовут %s.".format(nickname))
		else
			response("Выяснилось, что нашего питомца при жизни звали %s.".format(nickname))
	}

	def response(message: String) = room ! PetResponse(message)

	def savePet() {
		val state = PetStatus(nickname, alive, health, hunger)
		for (reply <- core ? StoreObject("pet", Some(roomName), state)) {
			reply match {
				case StoreOkReply =>
			}
		}
	}
}
