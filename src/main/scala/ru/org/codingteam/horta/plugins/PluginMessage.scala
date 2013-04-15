package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{CommandContext, Scope}

/**
 * Standard plugin messages that expected to be supported for any plugins.
 */
abstract sealed class PluginMessage

/**
 * This is the first message for plugin to respond. Must be responded with List[CommandDefinition] - collection of
 * command tokens and corresponding scopes.
 */
case class GetCommands() extends PluginMessage

/**
 * A process command request.
 * @param token token used when registering a command.
 * @param scope a scope in which command was resolved.
 * @param context a context for command (session context, user context, etc.).
 * @param arguments command argument array.
 */
case class ProcessCommand(token: Any, scope: Scope, context: CommandContext, arguments: Array[String])
