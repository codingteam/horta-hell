package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.AccessLevel

/**
 * Command definition.
 * @param level minimal access level.
 * @param name command name.
 * @param token command token (to be sent to plugin).
 */
case class CommandDefinition(level: AccessLevel, name: String, token: Any)
