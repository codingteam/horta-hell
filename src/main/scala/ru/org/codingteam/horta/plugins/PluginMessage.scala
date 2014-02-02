package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.Credential

/**
 * Standard plugin messages that expected to be supported for any plugins.
 */
abstract sealed class PluginMessage

/**
 * This is the first message for plugin to respond. Must be responded with List[CommandDefinition] - collection of
 * command tokens and corresponding scopes.
 */
case object GetCommands extends PluginMessage

/**
 * A process command request.
 * @param user a user executing the command.
 * @param token token used when registering a command.
 * @param arguments command argument array.
 */
case class ProcessCommand(user: Credential, token: Any, arguments: Array[String])
