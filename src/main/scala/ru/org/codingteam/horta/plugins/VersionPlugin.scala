package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.core.Core
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}

private object VersionCommand

class VersionPlugin extends BasePlugin with CommandProcessor {

  override def name = "version"

  override def commands = List(CommandDefinition(CommonAccess, "version", VersionCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) = {
    token match {
      case VersionCommand =>
        val version = Option(classOf[Core].getPackage.getImplementationVersion).getOrElse("development")
        Protocol.sendResponse(credential.location, credential, version)
      case _ =>
    }
  }

}
