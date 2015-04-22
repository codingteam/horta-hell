package ru.org.codingteam.horta

import java.nio.file.Paths

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Core
import scalikejdbc.GlobalSettings

object Application extends App with StrictLogging {

  override def main(args: Array[String]) {
    initializeConfiguration(args)

    val system = ActorSystem("CodingteamSystem", ConfigFactory.parseResources("application.conf"))
    val core = system.actorOf(Props[Core], name = "core")
  }

  private def initializeConfiguration(args: Array[String]) {
    GlobalSettings.loggingSQLAndTime = GlobalSettings.loggingSQLAndTime.copy(singleLineMode = true)

    val configPath = args match {
      case Array(config, _*) => config
      case _ => "horta.properties"
    }

    Configuration.initialize(Paths.get(configPath))
  }
}
