package ru.org.codingteam.horta.messages

import akka.actor.ActorRef

abstract class CommonMessage
case class InitializePlugin(core: ActorRef, plugins: Map[String, ActorRef])
