package ru.org.codingteam.horta.security

/**
 * A user in command context.
 * @param jid user identifier. May be None if unknown.
 * @param room room name. May be None if command wasn't given in room context.
 * @param roomNick room nick. May be None if command wasn't given in room context.
 * @param roomPrivileges user room privileges. May be None if command wasn't given in room context.
 */
case class User (
	jid: Option[String],
	room: Option[String],
	roomNick: Option[String],
	roomPrivileges: Option[RoomPrivilegeLevel])
