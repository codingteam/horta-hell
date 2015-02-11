package ru.org.codingteam.horta.protocol.jabber

abstract sealed class Role
case object Moderator extends Role
case object Participant extends Role
case object Visitor extends Role

