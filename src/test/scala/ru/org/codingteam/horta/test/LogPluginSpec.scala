package ru.org.codingteam.horta.test

import akka.actor.Props
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.plugins.log.{LogPlugin, SearchLogCommand}
import ru.org.codingteam.horta.plugins.{ProcessCommand, ProcessMessage}
import ru.org.codingteam.horta.protocol.SendResponse
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

class LogPluginSpec extends TestKitSpec {

  override val pluginProps = List(Props[LogPlugin])

  val credential = Credential(
    testActor,
    LocaleDefinition("en"),
    CommonAccess,
    Some("testroom"),
    "testuser",
    Some("testuser")
  )

  "LogPlugin" should {
    val plugin = plugins.head._1

    "save received message" in {
      plugin ! ProcessMessage(Clock.now, credential, "test")
      plugin ! ProcessCommand(credential, SearchLogCommand, Array("test"))
      val message = expectMsgType[SendResponse](timeout.duration)

      assert(message.text.contains("testuser "))
      assert(message.text.contains(" test"))
    }
  }
}
