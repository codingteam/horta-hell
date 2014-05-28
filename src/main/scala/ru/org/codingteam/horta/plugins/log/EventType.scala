package ru.org.codingteam.horta.plugins.log

abstract sealed class EventType(val name: String)
case object EnterType extends EventType("enter")
case object LeaveType extends EventType("leave")
case object MessageType extends EventType("message")
