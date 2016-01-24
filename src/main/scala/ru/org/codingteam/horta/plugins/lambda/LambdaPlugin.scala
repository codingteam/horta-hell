package ru.org.codingteam.horta.plugins.lambda

import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

private object LambdaCommand

class LambdaPlugin extends BasePlugin with CommandProcessor {
  /**
    * Plugin name.
    * @return unique plugin name.
    */
  override protected def name: String = "lambda"

  override protected def commands = List(CommandDefinition(CommonAccess, "lambda", LambdaCommand))

  /**
    * Process a command.
    * @param credential a credential of a user executing the command.
    * @param token token registered for command.
    * @param arguments command argument array.
    */
  override protected def processCommand(credential: Credential,
                                        token: Any,
                                        arguments: Array[String]): Unit = {
    implicit val c = credential
    implicit val l = log

    (token, arguments) match {
      case (LambdaCommand, Array(unparsedTerm, _*)) =>
        Protocol.sendResponse(credential.location, credential, "khooy")
      case _ => // ignore
    }
  }
}
