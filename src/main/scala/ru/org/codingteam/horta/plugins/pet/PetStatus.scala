package ru.org.codingteam.horta.plugins.pet

case class PetStatus(nickname: String, alive: Boolean, health: Int, hunger: Integer, coins: Map[String, Int])
