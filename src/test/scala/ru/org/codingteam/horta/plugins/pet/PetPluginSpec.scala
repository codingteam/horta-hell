package ru.org.codingteam.horta.plugins.pet

import akka.testkit.TestActorRef
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.test.TestKitSpec

class PetPluginSpec extends TestKitSpec {

  override def configure(): Unit = {
    val content = configuration +
      """
        |
        |rooms=room1,room2,room3
        |room1.room=foo@example.com
        |room2.room=bar@example.com
        |room3.room=baz@example.com
        |pet.rooms=room1,room2
      """.stripMargin
    Configuration.initialize(content)
  }

  def createPlugin() = TestActorRef[PetPlugin].underlyingActor

  "PetPlugin" should {
    "create a pet for room included in config" in {
      val plugin = createPlugin()
      val room = "bar@example.com"
      plugin.processRoomJoin(Clock.now, room, testActor)
      plugin.pets should contain key room
    }

    "not create a pet for room excluded from config" in {
      val plugin = createPlugin()
      val room = "baz@example.com"
      plugin.processRoomJoin(Clock.now, room, testActor)
      plugin.pets shouldNot contain key room
    }
  }
}
