package ru.org.codingteam.horta.plugins.pet

import scala.math._

object PtcUtils {
  def updatePTC(username: String, coins: Map[String, Int], value: Int) = {
    val userCoins = coins.getOrElse(username, 0)
    coins.updated(username, max(0, userCoins + value))
  }

  def getPTC(username: String, coins: Map[String, Int]) = {
    coins.getOrElse(username, 0)
  }
}
