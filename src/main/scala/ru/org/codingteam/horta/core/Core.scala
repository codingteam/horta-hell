package ru.org.codingteam.horta.actors.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.actors.database._
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.security._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import ru.org.codingteam.horta.protocol.jabber.JabberProtocol

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

	val parsers = List(SlashParsers, DollarParsers)

	override def preStart() {
		commands = commandDefinitions()
		commands foreach (command => log.info(s"Registered command: $command"))

		val protocol = context.actorOf(Props[JabberProtocol], "jabber")
		val store = context.actorOf(Props[PersistentStore], "store")
	}

	def receive = {
		// TODO: Receive message, parse command.
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

  private def accessGranted(user: Credential, access: AccessLevel) = {
    access match {
      case GlobalAccess => user.access == GlobalAccess
      case RoomAdminAccess => user.access == GlobalAccess || user.access == RoomAdminAccess
      case CommonAccess => true
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
	private def executeCommand(sender: ActorRef, user: Credential, name: String, arguments: Array[String]) {
    // TODO: Remove this?
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
