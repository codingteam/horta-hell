package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{RoomAdminAccess, GlobalAccess, Credential, CommonAccess}
import ru.org.codingteam.horta.localization.Localization._

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
      case AccessCommand =>
        val message = credential.access match {
          case GlobalAccess => "Global admin"
          case RoomAdminAccess => "Room admin"
          case CommonAccess => "Common user"
        }

        Protocol.sendResponse(credential.location, credential, localize(message)(credential))
      case _ =>
    }
  }

}
