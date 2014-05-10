package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.messages.SendResponse
import ru.org.codingteam.horta.core.Core

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
        credential.location ! SendResponse(credential, version)
      case _ =>
    }
  }

}
