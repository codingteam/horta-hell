package ru.org.codingteam.horta.actors.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import ru.org.codingteam.horta.actors.database.{StoreObject, ReadObject, PersistentStore}
import ru.org.codingteam.horta.actors.messenger.Messenger
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.plugins.{TestPlugin, FortunePlugin, CommandDefinition, GetCommands}
import ru.org.codingteam.horta.security._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.Some

class Core extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	/**
	 * List of plugin props to be started.
	 */
	val plugins: List[Props] = List(Props[TestPlugin], Props[FortunePlugin])

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
		commands foreach (command => log.info(s"Registered command: $command"))

		val messenger = context.actorOf(Props(new Messenger(self)), "messenger")
		pluginMap = Map("messenger" -> messenger)
		store = context.actorOf(Props(new PersistentStore(pluginMap)), "persistent_store")
	}

	def receive = {
		case RegisterCommand(command, role, receiver) => {
			commandMap = commandMap.updated(command, Command(command, role, receiver))
		}

		case ProcessCommand(user, message) => {
			val command = parseCommand(message)
			command match {
				case Some((name, arguments)) =>
					val scope = GlobalScope // TODO: properly determine scope.
					executeCommand(sender, scope, user, name, arguments)

					// TODO: Remove this deprecated mechanism:
					commandMap.get(name) match {
						case Some(Command(name, role, target)) if (accessGranted(user, role)) =>
							target ! ExecuteCommand(user, name, arguments)
						case None =>
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

	private def accessGranted(user: User, role: UserRole) = {
		role match {
			case BotOwner => user.role == BotOwner
			case KnownUser => user.role == BotOwner || user.role == KnownUser
			case UnknownUser => user.role == BotOwner || user.role == KnownUser || user.role == UnknownUser
		}
	}

	private def parseCommand(message: String): Option[(String, Array[String])] = {
		for (p <- parsers) {
			p.parse(p.command, message) match {
				case p.Success((name, arguments), _) => return Some((name.asInstanceOf[String], arguments.asInstanceOf[Array[String]]))
				case _ =>
			}
		}

		None
	}

	/**
	 * Executes the command.
	 * @param scope scope in which the command was received. May differ from the execution scope.
	 * @param user user that has sent the command.
	 * @param name command name.
	 * @param arguments command arguments.
	 */
	private def executeCommand(sender: ActorRef, scope: Scope, user: User, name: String, arguments: Array[String]) {
		val executors = commands.get(name)
		executors match {
			case Some(executors) =>
				executors foreach {
					case (plugin, CommandDefinition(commandScope, _, token)) =>
						val executionScope = commandScope // TODO: determine proper scope.
						val executionContext = CommandContext(self) // TODO: get proper execution context
						val request = ru.org.codingteam.horta.plugins.ProcessCommand(
								token,
								executionScope,
								executionContext,
								arguments)
						val roomFuture = ask(user.location, GetJID()).mapTo[String]
						val messageFuture = ask(plugin, request).mapTo[Option[String]].filter(_.isDefined)

						// Send the messages.
						for {
							room <- roomFuture
							message <- messageFuture
						} {
							val response = SendMucMessage(room, message.get) // TODO: More general response type.
							sender ! response
						}
				}
			case None =>
		}

	}
}
