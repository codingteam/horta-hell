package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.Credential

/**
 * Standard plugin messages that expected to be supported for any plugins.
 */
abstract sealed class PluginMessage

/**
 * This is the first message for plugin to respond. Must be responded with PluginDefinition.
 */
case object GetPluginDefinition extends PluginMessage

/**
 * A process command request.
 * @param user a user executing the command.
 * @param token token used when registering a command.
 * @param arguments command argument array.
 */
case class ProcessCommand(user: Credential, token: Any, arguments: Array[String])

/**
 * A process message request. It can be send to the plugin if it was registered for global message processing.
 * @param user a user sending the message.
 * @param message a message sent.
 */
case class ProcessMessage(user: Credential, message: String)
