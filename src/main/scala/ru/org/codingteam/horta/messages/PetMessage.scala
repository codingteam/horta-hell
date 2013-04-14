package ru.org.codingteam.horta.messages

abstract class PetMessage

case class PetCommand(command: Array[String]) extends PetMessage

case object PetTick extends PetMessage
