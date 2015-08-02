package ru.org.codingteam.horta.plugins.pet

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.plugins.pet.commands._
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Plugin for managing the so-called pet. Distinct pet belongs to every room.
 */
class PetPlugin extends BasePlugin with CommandProcessor with RoomProcessor with DataAccessingPlugin[PetRepository] {

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  var pets = Map[String, ActorRef]()

  val petCommands = {
    val withoutHelp = Map(
      "rating" -> new RatingCommand,
      "stats" -> new StatsCommand,
      "kill" -> new KillCommand,
      "resurrect" -> new ResurrectCommand,
      "feed" -> new FeedCommand,
      "heal" -> new HealCommand,
      "change-nick" -> new ChangeNickCommand,
      "coins" -> new CoinsCommand,
      "transfer" -> new TransferCommand
    )

    withoutHelp.updated("help", new HelpCommand(withoutHelp.keys.toList))
  }

  object PetCommandMatcher {
    def unapply(commandName: String): Option[AbstractCommand] =
      petCommands.get(commandName)
  }

  override def name = PetPlugin.name

  override def commands = List(CommandDefinition(CommonAccess, "pet", null))

  override protected val schema = "pet"
  override protected val createRepository = PetRepository.apply _

  override def processCommand(credential: Credential, token: Any, arguments: Array[String]) {
    val location = credential.location

    credential.roomId match {
      case Some(room) =>
        val pet = initializePet(room, location)

        // (isPrivate, text):
        val responseF: Future[(Boolean, String)] = arguments match {
          case Array(PetCommandMatcher(command), args@_*) =>
            (pet ? Pet.ExecuteCommand(command, credential, args.toArray)).mapTo[String].map(s => (false, s))
          case Array("transactions") =>
            withDatabase(_.readTransactions(credential.roomId.get, credential.name)) map { case transactions =>
              (true, transactions.mkString("\n"))
            }
          case _ => Future.successful((false, Localization.localize("Try $pet help.")(credential)))
        }

        for (response <- responseF) {
          response match {
            case (true, message) => Protocol.sendPrivateResponse(location, credential, message)
            case (false, message) => Protocol.sendResponse(location, credential, message)
          }
        }
      case None =>
    }
  }

  override def processRoomJoin(time: DateTime, roomJID: String, actor: ActorRef) {
    initializePet(roomJID, actor)
  }

  override def processRoomLeave(time: DateTime, roomJID: String) {
    pets.get(roomJID) match {
      case Some(pet) =>
        context.stop(pet)
        pets -= roomJID
      case None =>
    }
  }

  private def initializePet(roomId: String, location: ActorRef): ActorRef = {
    pets.get(roomId) match {
      case Some(actor) => actor
      case None =>
        val actor = context.actorOf(Props(classOf[Pet], roomId, location))
        pets = pets.updated(roomId, actor)
        actor
    }
  }

}

object PetPlugin {
  def name = "pet"
}
