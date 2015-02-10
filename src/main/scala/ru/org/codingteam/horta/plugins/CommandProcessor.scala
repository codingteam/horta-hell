package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.Credential

/**
 * Trait for plugins that can process user commands.
 */
trait CommandProcessor extends BasePlugin {

  override def receive = {
    case ProcessCommand(credential, token, arguments) => processCommand(credential, token, arguments)
    case other => super.receive(other)
  }

  /**
   * Process a command.
   * @param credential a credential of a user executing the command.
   * @param token token registered for command.
   * @param arguments command argument array.
   */
  protected def processCommand(credential: Credential,
                               token: Any,
                               arguments: Array[String]): Unit

}
