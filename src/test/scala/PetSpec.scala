import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import ru.org.codingteam.horta.plugins.pet.{PetData, Pet}
import ru.org.codingteam.horta.plugins.pet.Pet.{GetPetDataInternal, SetPetDataInternal}
import scala.concurrent.duration._

class PetSpec extends TestKit(ActorSystem("TestSystem", ConfigFactory.parseString(
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
  val stubReceiver = system.actorOf(Props(new Actor with ActorLogging {
    override def receive = LoggingReceive {
      case _ =>
    }
  }), "stubReceiver")

  val petActor = system.actorOf(Props(classOf[Pet], "roomId", stubReceiver))

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
