package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.Scope

/**
 * Command definition.
 * @param scope command scope.
 * @param name command name.
 * @param token command token (to be sent to plugin).
 */
case class CommandDefinition(scope: Scope, name: String, token: Any)
