package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}

private object AccessCommand

/**
 * Access test plugin. Its work is to respond user privileges to any request.
 */
class AccessPlugin extends BasePlugin with CommandProcessor {

  override def name = "access"

  override def commands = List(CommandDefinition(CommonAccess, "access", AccessCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) = {
    token match {
      case AccessCommand => Protocol.sendResponse(credential.location, credential, credential.access.toString)
      case _ =>
    }
  }

}
