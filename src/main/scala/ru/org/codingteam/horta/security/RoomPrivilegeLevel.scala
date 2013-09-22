package ru.org.codingteam.horta.security

/**
 * The user's privileges in current room context.
 */
abstract class RoomPrivilegeLevel

/**
 * Room visitor.
 */
case object RoomVisitor extends RoomPrivilegeLevel

/**
 * Room member.
 */
case object RoomMember extends RoomPrivilegeLevel

/**
 * User with temporary (session-scoped) admin privileges.
 */
case object RoomTemporaryAdmin extends RoomPrivilegeLevel

/**
 * User with admin privileges.
 */
case object RoomAdmin extends RoomPrivilegeLevel

/**
 * User with owner privileges.
 */
case object RoomOwner extends RoomPrivilegeLevel
