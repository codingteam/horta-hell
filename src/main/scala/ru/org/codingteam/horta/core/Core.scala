package ru.org.codingteam.horta.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.actors.database._
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.security._
import scala.concurrent.Lock
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import ru.org.codingteam.horta.protocol.jabber.JabberProtocol
import ru.org.codingteam.horta.plugins.markov.MarkovPlugin
import ru.org.codingteam.horta.plugins.pet.PetPlugin
import ru.org.codingteam.horta.plugins.bash.BashPlugin

class Core extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  val delayLock = new Lock()

  /**
   * List of plugin props to be started.
   */
  val plugins: List[Props] = List(
    Props[TestPlugin],
    Props[FortunePlugin],
    Props[AccessPlugin],
    Props[PetPlugin],
    Props[MarkovPlugin],
    Props[VersionPlugin],
    Props[BashPlugin]
  )

  /**
   * List of registered commands.
   */
  var commands = Map[String, List[(ActorRef, CommandDefinition)]]()

  val parsers = List(SlashParsers, DollarParsers)

  override def preStart() {
    commands = commandDefinitions()
    commands foreach (command => log.info(s"Registered command: $command"))

    // TODO: What is the Akka way to create these?
    val protocol = context.actorOf(Props[JabberProtocol], "jabber")
    val store = context.actorOf(Props[PersistentStore], "store")
  }

  def receive = {
    case CoreMessage(credential, text) => {
      delayLock.acquire()
      val command = parseCommand(text)
      command match {
        case Some((name, arguments)) =>
          executeCommand(sender, credential, name, arguments)
        case None =>
      }

      context.system.scheduler.scheduleOnce(1 second) {
        delayLock.release()
      }
    }
  }

  private def commandDefinitions(): Map[String, List[(ActorRef, CommandDefinition)]] = {
    val commandRequests = Future.sequence(
      for (plugin <- plugins) yield {
        val actor = context.actorOf(plugin)
        ask(actor, GetCommands).mapTo[List[CommandDefinition]].map(
          definitions => definitions.map(
            definition => (actor, definition)))
      })

    val results = Await.result(commandRequests, 60 seconds)
    val definitions = results.flatten
    val groups = definitions.groupBy {
      case (_, CommandDefinition(_, name, _)) => name
    }

    groups
  }

  private def parseCommand(message: String): Option[(String, Array[String])] = {
    for (p <- parsers) {
      p.parse(p.command, message) match {
        case p.Success((name, arguments), _) => return Some((name.asInstanceOf[String], arguments.asInstanceOf[Array[String]]))
        case _ =>
      }
    }

    None
  }

  /**
   * Executes the command.
   * @param credential credential of user who has sent the command.
   * @param name command name.
   * @param arguments command arguments.
   */
  private def executeCommand(sender: ActorRef, credential: Credential, name: String, arguments: Array[String]) {
    val executors = commands.get(name)
    executors match {
      case Some(executors) =>
        executors foreach {
          case (plugin, CommandDefinition(level, _, token)) if accessGranted(level, credential) =>
            plugin ! ProcessCommand(credential, token, arguments)
        }
      case None =>
    }
  }

  private def accessGranted(access: AccessLevel, user: Credential) = {
    access match {
      case GlobalAccess => user.access == GlobalAccess
      case RoomAdminAccess => user.access == GlobalAccess || user.access == RoomAdminAccess
      case CommonAccess => true
    }
  }
}
