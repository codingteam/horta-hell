package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.messages.SendResponse

private object AccessCommand

/**
 * Access test plugin. Its work is to respond user privileges to any request.
 */
class AccessPlugin extends CommandPlugin {
	def pluginDefinition = PluginDefinition(
    false,
    List(CommandDefinition(CommonAccess, "access", AccessCommand)))

	override def processCommand (
		credential: Credential,
		token: Any,
		arguments: Array[String]) = {
		token match {
			case AccessCommand => credential.location ! SendResponse(credential, credential.access.toString)
			case _ =>
		}
	}

}
