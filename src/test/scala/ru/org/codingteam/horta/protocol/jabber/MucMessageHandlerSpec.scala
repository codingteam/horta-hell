package ru.org.codingteam.horta.protocol.jabber

import akka.actor.Props
import ru.org.codingteam.horta.localization.LocaleDefinition
import ru.org.codingteam.horta.messages.UserJoined
import ru.org.codingteam.horta.protocol.{SendMucMessage, SendResponse}
import ru.org.codingteam.horta.security.{CommonAccess, Credential}
import ru.org.codingteam.horta.test.TestKitSpec

class MucMessageHandlerSpec extends TestKitSpec {

  val room = "codingteam@conference.codingteam.org.ru"
  val locale = LocaleDefinition("en")
  val handler = system.actorOf(
    Props(
      classOf[MucMessageHandler],
      locale,
      testActor,
      room,
      "horta-hell"))

  "MucMessageHandler" should {
    "replace nickname of existing participant" in {
      val sender = "nickname"
      val target = "user"
      val senderId = s"$room/$sender"
      val targetId = s"$room/$target"
      handler ! UserJoined(targetId, Owner, Moderator)
      handler ! SendResponse(Credential(handler, locale, CommonAccess, Some(room), sender, Some(senderId)), target)

      val response = expectMsgType[SendMucMessage](timeout.duration)
      assert(response.message === s"$sender: u-er")
    }
  }
}
