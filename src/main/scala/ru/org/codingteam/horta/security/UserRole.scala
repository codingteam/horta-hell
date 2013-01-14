package ru.org.codingteam.horta.security

abstract class UserRole
case object BotOwner extends UserRole
case object KnownUser extends UserRole
case object UnknownUser extends UserRole