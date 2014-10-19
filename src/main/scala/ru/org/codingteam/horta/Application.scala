package ru.org.codingteam.horta

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.Logging
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Core

object Application extends App with Logging {

  override def main(args: Array[String]) {
    initializeConfiguration(args)

    val system = ActorSystem("CodingteamSystem", ConfigFactory.parseResources("application.conf"))
    val core = system.actorOf(Props[Core], name = "core")
  }

  private def initializeConfiguration(args: Array[String]) {
    val configPath = args match {
      case Array(config, _*) => config
      case _ => "horta.properties"
    }

    Configuration.initialize(configPath)
  }
}
