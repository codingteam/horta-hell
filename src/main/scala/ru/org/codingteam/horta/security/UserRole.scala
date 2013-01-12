package ru.org.codingteam.horta.security

abstract class UserRole
case class BotOwner() extends UserRole
case class KnownUser() extends UserRole
case class UnknownUser() extends UserRole