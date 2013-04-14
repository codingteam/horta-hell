package ru.org.codingteam.horta.actors.core

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ru.org.codingteam.horta.actors.messenger.Messenger
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security._
import scala.Some
import ru.org.codingteam.horta.actors.database.{StoreObject, ReadObject, PersistentStore}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._

class Core extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	var commands = Map[String, Command]()
	var plugins: Map[String, ActorRef] = null
	val parsers = List(SlashParsers, DollarParsers)
	var store: ActorRef = null

	override def preStart() = {
		val messenger = context.actorOf(Props(new Messenger(self)), "messenger")
		plugins = Map("messenger" -> messenger)
		store = context.actorOf(Props(new PersistentStore(plugins)), "persistent_store")
	}

	def receive = {
		case RegisterCommand(command, role, receiver) => {
			commands = commands.updated(command, Command(command, role, receiver))
		}

		case ProcessCommand(user, message) => {
			val arguments = parseCommand(message)
			arguments match {
				case Some(CommandArguments(Command(name, role, target), args)) => {
					if (accessGranted(user, role)) {
						target ! ExecuteCommand(user, name, args)
					}
				}

				case None =>
			}
		}

		case ReadObject(plugin, id) => {
			store.ask(ReadObject(plugin, id)).pipeTo(sender)
		}

		case StoreObject(plugin, id, obj) => {
			store.ask(StoreObject(plugin, id, obj)).pipeTo(sender)
		}
	}

	def accessGranted(user: User, role: UserRole) = {
		role match {
			case BotOwner => user.role == BotOwner
			case KnownUser => user.role == BotOwner || user.role == KnownUser
			case UnknownUser => user.role == BotOwner || user.role == KnownUser || user.role == UnknownUser
		}
	}

	def parseCommand(message: String): Option[CommandArguments] = {
		for (p <- parsers) {
			p.parse(p.command, message) match {
				case p.Success((name, arguments), _) => commands.get(name) match {
					case Some(command) => return Some(CommandArguments(command, arguments))
					case None =>
				}
				case _ =>
			}
		}

		None
	}
}
