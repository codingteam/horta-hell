package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef

case class Pet(location: ActorRef, nickname: String, alive: Boolean, health: Integer, hunger: Integer)

object Pet {
  def default(location: ActorRef) = Pet(location, "Наркоман", true, 100, 100)
}
