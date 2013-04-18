package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{CommandContext, Scope, GlobalScope}

/**
 * Test plugin. Its work is to respond "test" to any test request.
 */
class TestPlugin extends CommandPlugin {

	private object TestCommand

	/**
	 * A collection of (token -> scope) pairs, where token defines command token and scope - its scope.
	 * @return collection.
	 */
	def commandDefinitions: List[CommandDefinition] = List(CommandDefinition(GlobalScope, "test", TestCommand))

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
		arguments: Array[String]): Option[String] = {
		token match {
			case TestCommand => Some("test")
			case _ => None
		}
	}

}
