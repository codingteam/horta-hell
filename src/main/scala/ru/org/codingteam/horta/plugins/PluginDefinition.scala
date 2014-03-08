package ru.org.codingteam.horta.plugins

/**
 * A plugin definition.
 * @param messageReceiver true if this plugin wants to receive every message in the system.
 * @param commands a list of commands supported by the plugin.
 */
case class PluginDefinition(messageReceiver: Boolean, commands: List[CommandDefinition])
