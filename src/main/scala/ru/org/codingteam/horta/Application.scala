package ru.org.codingteam.horta

import java.nio.file.Paths

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Core
import ru.org.codingteam.horta.events.{EventCollector, TwitterEndpoint}
import ru.org.codingteam.horta.plugins.bash.BashPlugin
import ru.org.codingteam.horta.plugins.diag.DiagnosticPlugin
import ru.org.codingteam.horta.plugins.dice.DiceRoller
import ru.org.codingteam.horta.plugins.helper.HelperPlugin
import ru.org.codingteam.horta.plugins.karma.KarmaPlugin
import ru.org.codingteam.horta.plugins.log.LogPlugin
import ru.org.codingteam.horta.plugins.loglist.LogListPlugin
import ru.org.codingteam.horta.plugins.mail.MailPlugin
import ru.org.codingteam.horta.plugins.markov.MarkovPlugin
import ru.org.codingteam.horta.plugins.pet.PetPlugin
import ru.org.codingteam.horta.plugins.visitor.VisitorPlugin
import ru.org.codingteam.horta.plugins.wtf.WtfPlugin
import ru.org.codingteam.horta.plugins.{AccessPlugin, FortunePlugin, TwitterPlugin, VersionPlugin}
import scalikejdbc.GlobalSettings

object Application extends App with StrictLogging {

  //TODO: This should be eventually handled by some kind of dependency injeciton
  val eventEndpoints = List(
    new TwitterEndpoint()
  )

  val plugins = List(
    Props[DiagnosticPlugin],
    Props[FortunePlugin],
    Props[AccessPlugin],
    Props[LogPlugin],
    Props[VisitorPlugin],
    Props[WtfPlugin],
    Props[MailPlugin],
    Props[PetPlugin],
    Props[MarkovPlugin],
    Props[VersionPlugin],
    Props[BashPlugin],
    Props[DiceRoller],
    // Props[HtmlReaderPlugin], // TODO: Disabled for security reeasons, see #366
    Props[HelperPlugin],
    Props[KarmaPlugin],
    Props[LogListPlugin],
    Props[TwitterPlugin]
  )

  initializeConfiguration(args)

  val system = ActorSystem("CodingteamSystem", ConfigFactory.parseResources("application.conf"))
  val core = system.actorOf(Props(new Core(plugins)), "core")
  val eventCollector = system.actorOf(Props(classOf[EventCollector], eventEndpoints))

  private def initializeConfiguration(args: Array[String]) {
    GlobalSettings.loggingSQLAndTime = GlobalSettings.loggingSQLAndTime.copy(singleLineMode = true)

    val configPath = args match {
      case Array(config, _*) => config
      case _ => "horta.properties"
    }

    Configuration.initialize(Paths.get(configPath))
  }
}
