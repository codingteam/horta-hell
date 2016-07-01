package ru.org.codingteam.horta.test

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Core
import ru.org.codingteam.horta.plugins.log.LogPlugin

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class TestKitSpec extends TestKit(ActorSystem("TestSystem", ConfigFactory.parseString(
  """
    |akka.loglevel = INFO
    |akka.actor.debug.receive = on
    |akka.actor.deployment {
    |}
  """.stripMargin)))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with OptionValues
  with Eventually {
  implicit val timeout = Timeout(15.seconds)

  val configuration =
    """
      |storage.url=jdbc:h2:hell_test;DB_CLOSE_DELAY=-1
      |storage.user=sa
      |storage.password=
      |""".stripMargin

  Configuration.initialize(configuration)

  val pluginProps = List[Props]()
  val core = system.actorOf(Props(new Core(List(Props[LogPlugin]), List())), "core")
  val plugins = Await.result(Core.getPluginDefinitions(core), timeout.duration)
}
