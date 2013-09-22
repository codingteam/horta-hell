package ru.org.codingteam.horta.actors.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import ru.org.codingteam.horta.actors.database._
import ru.org.codingteam.horta.actors.messenger.Messenger
import ru.org.codingteam.horta.actors.database.StoreObject
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.messages.ProcessCommand
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.security._
import ru.org.codingteam.horta.Configuration
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class Core extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	/**
	 * List of plugin props to be started.
	 */
	val plugins: List[Props] = List(Props[TestPlugin], Props[FortunePlugin], Props[AccessPlugin])

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
		case RegisterCommand(level, name, receiver) => {
			commandMap += name -> Command(level, name, receiver)
		}

		case ProcessCommand(user, message) => {
			val command = parseCommand(message)
			command match {
				case Some((name, arguments)) =>
					executeCommand(sender, user, name, arguments)

					// TODO: Remove this deprecated mechanism:
					commandMap.get(name) match {
						case Some(Command(level, name, target)) if (accessGranted(user, level)) =>
							sender ! ExecuteCommand(user, name, arguments)
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

	private def accessGranted(user: User, access: AccessLevel) = {
		if (user.jid == Configuration.owner) {
			true
		} else {
			access match {
				case GlobalAccess => false
				case RoomAdminAccess => user.roomPrivileges match {
					case Some(RoomTemporaryAdmin) | Some(RoomAdmin) | Some(RoomOwner) => true
					case _ => false
				}
				case CommonAccess => true
			}
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
	 * @param user user that has sent the command.
	 * @param name command name.
	 * @param arguments command arguments.
	 */
	private def executeCommand(sender: ActorRef, user: User, name: String, arguments: Array[String]) {
		val executors = commands.get(name)
		executors match {
			case Some(executors) =>
				executors foreach {
					case (plugin, CommandDefinition(level, _, token)) =>
						val request = ru.org.codingteam.horta.plugins.ProcessCommand(
							user,
							token,
							arguments)

						val messageFuture = ask(plugin, request).mapTo[Option[String]].filter(_.isDefined)

						// Send the messages.
						for {
							message <- messageFuture
						} {
							val text = message.get
							val response = user.room match {
								case Some(room) => SendMucMessage(room, text)
								case None => user.jid match {
									case Some(jid) => SendChatMessage(jid, text)
									case None =>
										log.info(s"Trying to send $text response but it's unknown how to send it")
										return
								}
							}

							sender ! response
						}
				}
			case None =>
		}

	}
}
