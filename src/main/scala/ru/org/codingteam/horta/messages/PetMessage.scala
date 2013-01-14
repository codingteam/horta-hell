package ru.org.codingteam.horta.messages

import akka.actor.ActorRef

abstract class PetMessage
case class PetCommand(command: Array[String]) extends PetMessage
case class PetTick extends PetMessage
