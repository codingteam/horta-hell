package ru.org.codingteam.horta.plugins.markov

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import me.fornever.platonus.Network
import org.joda.time.DateTime
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.plugins.log.LogRepository
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case object SayCommand
case object ReplaceCommand

class MarkovPlugin() extends BasePlugin
  with CommandProcessor
  with MessageProcessor
  with DataAccessingPlugin[LogRepository] {

  import context.dispatcher
  implicit val timeout = MarkovPlugin.timeout

  var users = Map[UserIdentity, ActorRef]()

  override def name = "markov"

  override protected val schema: String = "log"
  override protected val createRepository = LogRepository.apply _

  override def commands = List(
    CommandDefinition(CommonAccess, "say", SayCommand),
    CommandDefinition(CommonAccess, "s", ReplaceCommand)
  )

  override def receive: PartialFunction[Any, Unit] = {
    case MarkovPlugin.DisposeUser(identity) =>
      disposeUser(identity)
    case MarkovPlugin.ParseLogs(identity) =>
      val s = sender
      parseLogs(identity).onSuccess({ case network =>
        s ! network
      })
    case other =>
      super.receive(other)
  }

  override def processMessage(time: DateTime, credential: Credential, message: String) {
    val user = getUser(credential)
    user ! UserPhrase(message)
  }

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case SayCommand =>
        generatePhrase(credential, arguments)
      case ReplaceCommand =>
        replace(credential, arguments)
    }
  }

  def isMyself(credential: Credential): Boolean = {
    (Configuration.roomDescriptors find { rd => rd.room == credential.roomId.getOrElse("")} map { rd => rd.nickname} getOrElse (Configuration.dftName)) == credential.name
  }

  def generatePhrase(credential: Credential, arguments: Array[String]) {
    arguments match {
      case Array(_) | Array() =>
        val length = Math.max(1, arguments match {
          case Array(lengthParameter, _*) =>
            try {
              lengthParameter.toInt
            } catch {
              case _: NumberFormatException => 1
            }
          case _ => 1
        })

        if (!isMyself(credential)) {
          implicit val c = credential
          import Localization._
          val user = getUser(credential)
          val location = credential.location
          if (Math.random() > 0.99) {
            Protocol.sendResponse(location, credential, localize("pfshhh"))
            Protocol.sendResponse(location, credential, localize("shpfff"))
            Protocol.sendResponse(location, credential, localize("Luke, I am your Horta!"))
          } else if (Math.random() < 0.01) {
            Protocol.sendResponse(location, credential, localize("BLOOD GORE DESTROY"))
            for (i <- 1 to 10) {
              user ! GeneratePhrase(credential, 1, true)
            }
          } else {
            user ! GeneratePhrase(credential, length, false)
          }
        }

      case _ =>
    }
  }

  def replace(credential: Credential, arguments: Array[String]) {
    val location = credential.location

    if (!isMyself(credential)) {
      arguments match {
        case Array(from, to, _*) if from != "" => {
          val user = getUser(credential)
          user ! ReplaceRequest(credential, from, to)
        }

        case _ =>
          Protocol.sendResponse(location, credential, Localization.localize("Invalid arguments.")(credential))
      }
    }
  }

  def getUser(credential: Credential) = {
    val roomName = credential.roomId.getOrElse("")
    val name = credential.name
    val identity = UserIdentity(roomName, name)

    val user = users.get(identity)
    user match {
      case Some(u) => u
      case None =>
        val user = context.actorOf(Props(new MarkovUser(roomName, name)))
        users = users.updated(identity, user)
        user
    }
  }

  private def disposeUser(identity: UserIdentity): Unit = {
    log.info(s"Disposing user $identity")
    users -= identity
  }

  private def parseLogs(identity: UserIdentity): Future[Network] = {
    withDatabase(repository => LogParser.parse(repository, identity.room, identity.nickname))
  }
}

object MarkovPlugin {
  case class DisposeUser(identity: UserIdentity)
  case class ParseLogs(identity: UserIdentity)

  implicit val timeout = Timeout(5.minutes)

  def disposeUser(plugin: ActorRef, identity: UserIdentity): Unit = {
    plugin ! DisposeUser(identity)
  }

  def parseLogs(plugin: ActorRef, identity: UserIdentity): Future[Network] = {
    plugin.ask(ParseLogs(identity)).mapTo[Network]
  }
}
