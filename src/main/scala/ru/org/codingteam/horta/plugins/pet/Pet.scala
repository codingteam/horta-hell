package ru.org.codingteam.horta.plugins.pet

import akka.actor.ActorRef

case class Pet(location: Option[ActorRef],
               nickname: String,
               alive: Boolean,
               health: Int,
               hunger: Integer,
               coins: Map[String, Int])

object Pet {
  def default(location: ActorRef) = Pet(Some(location), "Наркоман", true, 100, 100, Map[String, Int]())
}
