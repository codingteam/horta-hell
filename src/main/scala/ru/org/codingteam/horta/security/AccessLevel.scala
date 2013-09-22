package ru.org.codingteam.horta.security

/**
 * User access level.
  */
abstract class AccessLevel

/**
 * Global administrator access.
 */
case object GlobalAccess extends AccessLevel

/**
 * Room administrator access.
 */
case object RoomAdminAccess extends AccessLevel

/**
 * Ordinar user access.
 */
case object CommonAccess extends AccessLevel
