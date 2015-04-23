package ru.org.codingteam.horta.test

import akka.actor.Props
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.database.{PersistentStore, RepositoryFactory}
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.plugins.log.{LogRepository, LogPlugin, SearchLogCommand}
import ru.org.codingteam.horta.plugins.{ProcessCommand, ProcessMessage}
import ru.org.codingteam.horta.protocol.SendMucMessage
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

class LogPluginSpec extends TestKitSpec {

  val store = system.actorOf(
    Props(classOf[PersistentStore], Map(("log", RepositoryFactory("log", LogRepository.apply)))),
    "store" // TODO: Emulate proper path
  )
  val plugin = system.actorOf(Props[LogPlugin]())
  val credential = Credential(
    stubReceiver,
    LocaleDefinition("en"),
    CommonAccess,
    Some("testroom"),
    "testuser",
    Some("testuser")
  )

  "LogPlugin" should {
    "save received message" in {
      plugin ! ProcessMessage(Clock.now, credential, "test")
      plugin ! ProcessCommand(credential, SearchLogCommand, Array("test"))
      val message = expectMsgType[SendMucMessage]

      assert(message.toJID === "testroom")
      assert(message.message.contains("testuser: "))
      assert(message.message.contains(" test"))
    }
  }
}
