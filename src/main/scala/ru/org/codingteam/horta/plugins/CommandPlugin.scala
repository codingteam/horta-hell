package ru.org.codingteam.horta.plugins

import akka.actor.{Actor, ActorLogging}
import ru.org.codingteam.horta.security.{Scope, CommandContext}

/**
 * CommandPlugin trait used as base for all command plugins.
 */
abstract class CommandPlugin extends Actor with ActorLogging {
	def receive = {
		case StartPlugin() => sender ! commandDefinitions
		case ProcessCommand(token, scope, context, arguments) =>
			sender ! processCommand(token, scope, context, arguments)
	}

	/**
	 * A collection of (token -> scope) pairs, where token defines command token and scope - its scope.
	 * @return collection.
	 */
	def commandDefinitions: List[(Any, Scope)]

	/**
	 * Process a command.
	 * @param token token registered for command.
	 * @param scope a scope in which command was resolved.
	 * @param context context of command.
	 * @param arguments command argument array.
	 * @return string for replying the sender.
	 */
	def processCommand (
		token: Any,
		scope: Scope,
		context: CommandContext,
		arguments: Array[String]): Option[String]
}
