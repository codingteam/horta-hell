package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{User, CommonAccess}

/**
 * Access test plugin. Its work is to respond user privileges to any request.
 */
class AccessPlugin extends CommandPlugin {

	private object AccessCommand

	/**
	 * A collection of (token -> scope) pairs, where token defines command token and scope - its scope.
	 * @return collection.
	 */
	def commandDefinitions: List[CommandDefinition] = List(CommandDefinition(CommonAccess, "access", AccessCommand))

	/**
	 * Process a command.
	 * @param user a user executing the command.
	 * @param token token registered for command.
	 * @param arguments command argument array.
	 * @return string for replying the sender.
	 */
	def processCommand (
		user: User,
		token: Any,
		arguments: Array[String]): Option[String] = {
		token match {
			case AccessCommand => Some(user.toString)
			case _ => None
		}
	}

}
