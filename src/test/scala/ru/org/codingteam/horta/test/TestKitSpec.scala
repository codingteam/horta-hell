package ru.org.codingteam.horta.test

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import ru.org.codingteam.horta.configuration.Configuration

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
  Configuration.initialize("")
  val stubReceiver = system.actorOf(Props(new Actor with ActorLogging {
    override def receive = LoggingReceive {
      case _ =>
    }
  }), "stubReceiver")
}
