package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.localization.Localization._

private object FortuneCommand

class FortunePlugin extends BasePlugin with CommandProcessor {

  override def name = "fortune"

  override def commands = List(CommandDefinition(CommonAccess, "fortune", FortuneCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    implicit val c = credential
    token match {
      case FortuneCommand =>
        Protocol.sendResponse(credential.location, credential,
          localize("Fortune plugin is deprecated. You may take a look at $loglist command for similar functionality."))
    }
  }

}
