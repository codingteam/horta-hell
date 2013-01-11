package ru.org.codingteam.horta.messages

import akka.actor.ActorRef

abstract class ParserMessage
case class InitializeParser(roomName: String, roomActor: ActorRef) extends ParserMessage
