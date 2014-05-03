package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.actors.database.DAO

/**
 * A plugin definition.
 * @param name plugin name.
 * @param messageReceiver true if this plugin wants to receive every message in the system.
 * @param commands a list of commands supported by the plugin.
 * @param dao plugin data access object if present.
 */
case class PluginDefinition(name: String,
                            messageReceiver: Boolean,
                            commands: List[CommandDefinition],
                            dao: Option[DAO])
