package ru.org.codingteam.horta.plugins.visitor

import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.security.{RoomAdminAccess, Credential}

object VisitorsCommand

class VisitorPlugin extends CommandProcessor {

  /**
   * A collection of command definitions.
   * @return a collection.
   */
  override protected def commands: List[CommandDefinition] =
    List(CommandDefinition(RoomAdminAccess, "visitors", VisitorsCommand))

  /**
   * Process a command.
   * @param credential a credential of a user executing the command.
   * @param token token registered for command.
   * @param arguments command argument array.
   * @return string for replying the sender.
   */
  override protected def processCommand(credential: Credential, token: Any, arguments: Array[String]): Unit = {
    credential.location
  }

  /**
   * Plugin name.
   * @return unique plugin name.
   */
  override protected def name: String = ???

}
