package ru.org.codingteam.horta.actors.core

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import scala.language.postfixOps
import ru.org.codingteam.horta.actors.messenger.Messenger
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security._
import scala.Some
import ru.org.codingteam.horta.actors.database.{StoreObject, ReadObject, PersistentStore}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._
import ru.org.codingteam.horta.plugins.{CommandDefinition, GetCommands}
import akka.dispatch.Futures
import scala.concurrent.{Future, Await}

class Core extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	/**
	 * List of plugin props to be started.
	 */
	val plugins: List[Props] = List()

	/**
	 * List of registered commands.
	 */
	var commands = Map[String, List[(ActorRef, CommandDefinition)]]()

	@Deprecated
	var commandMap = Map[String, Command]()

	@Deprecated
	var pluginMap: Map[String, ActorRef] = null
	val parsers = List(SlashParsers, DollarParsers)
	var store: ActorRef = null

	override def preStart() {
		commands = commandDefinitions()

		val messenger = context.actorOf(Props(new Messenger(self)), "messenger")
		pluginMap = Map("messenger" -> messenger)
		store = context.actorOf(Props(new PersistentStore(pluginMap)), "persistent_store")
	}

	def receive = {
		case RegisterCommand(command, role, receiver) => {
			commandMap = commandMap.updated(command, Command(command, role, receiver))
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

	private def commandDefinitions(): Map[String, List[(ActorRef, CommandDefinition)]] = {
		val commandRequests = Future.sequence(
			for (plugin <- plugins) yield {
				val actor = context.actorOf(plugin)
				ask(actor, GetCommands()).mapTo[List[CommandDefinition]].map(
					definitions => definitions.map(
						definition => (actor, definition)))
			})

		val results = Await.result(commandRequests, 60 seconds)
		val definitions = results.flatten
		val groups = definitions.groupBy {
			case (_, CommandDefinition(_, name, _)) => name
		}

		groups
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
				case p.Success((name, arguments), _) => commandMap.get(name) match {
					case Some(command) => return Some(CommandArguments(command, arguments))
					case None =>
				}
				case _ =>
			}
		}

		None
	}
}
