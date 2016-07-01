package ru.org.codingteam.horta.plugins.pet

import akka.actor.Props
import org.joda.time.DateTime
import ru.org.codingteam.horta.plugins.pet.Pet.{GetPetDataInternal, SetPetDataInternal}
import ru.org.codingteam.horta.test.TestKitSpec

import scala.concurrent.duration._

class PetSpec extends TestKitSpec {

  val petActor = system.actorOf(Props(classOf[Pet], "roomId", testActor))

  "Pet" should {
    "Save & return PetData" in within (500.millis) {
      petActor ! GetPetDataInternal
      expectMsgType[Option[PetData]] should be (None)
      val petData = PetData("uggur", alive = false, health = 0, satiation = 0, DateTime.now)
      petActor ! SetPetDataInternal(petData)
      eventually {
        petActor ! GetPetDataInternal
        expectMsgType[Option[PetData]].value should be (petData)
      }
    }
  }
}
