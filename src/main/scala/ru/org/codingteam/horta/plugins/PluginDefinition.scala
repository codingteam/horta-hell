package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.database.DAO

/**
 * Description of events.
 * @param messages true if plugin want to be notified on message receival.
 * @param rooms true if plugin want to be notified on room entering / leaving.
 * @param participants true if plugin want to be notified on participant entering / leaving the room.
 */
case class Notifications(messages: Boolean,
                         rooms: Boolean,
                         participants: Boolean)

/**
 * A plugin definition.
 * @param name plugin name.
 * @param notifications description of events plugin want to be notified of.
 * @param commands a list of commands supported by the plugin.
 * @param dao plugin data access object if present.
 */
case class PluginDefinition(name: String,
                            notifications: Notifications,
                            commands: List[CommandDefinition],
                            dao: Option[DAO])
