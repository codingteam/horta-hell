package ru.org.codingteam.horta.core

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime
import ru.org.codingteam.horta.database.{PersistentStore, RepositoryFactory}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.plugins._
import ru.org.codingteam.horta.protocol.IProtocol
import ru.org.codingteam.horta.protocol.xmpp.XmppProtocol
import ru.org.codingteam.horta.security._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
 * Horta core actor. Manages all plugins, routes global messages.
 */
class Core(pluginProps: List[Props]) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)
  val parsers = List(SlashParsers, DollarParsers)
  /**
   * List of registered commands.
   */
  var commands = Map[String, List[(ActorRef, CommandDefinition)]]()
  /**
   * List of plugins receiving all the messages.
   */
  var messageReceivers = List[ActorRef]()
  /**
   * List of plugins receiving room notifications.
   */
  var roomReceivers = List[ActorRef]()
  /**
   * Initialized plugin definitions and corresponding actor references.
   */
  var definitions = List[(ActorRef, PluginDefinition)]()
  /**
   * List of plugins' subscriptions to external events
   */
  var subscriptions = List[Subscribe]()
  /**
   * List of plugins receiving the user notifications.
   */
  var participantReceivers = List[ActorRef]()

  private var protocols = Vector[IProtocol]()

  override def preStart() {
    definitions = getPluginDefinitions
    parseNotifications(definitions)

    commands = Core.getCommands(definitions)
    commands foreach (command => log.info(s"Registered command: $command"))

    val storages = Core.getStorages(definitions)
    val store = context.actorOf(Props(classOf[PersistentStore], storages), "store")
    protocols = Vector(new XmppProtocol(log, self))
  }

  override def postStop(): Unit = {
    protocols.foreach(_.dispose())
    super.postStop()
  }

  private def getPluginDefinitions: List[(ActorRef, PluginDefinition)] = {
    val responses = Future.sequence(for (plugin <- pluginProps) yield {
      val actor = context.actorOf(plugin)
      ask(actor, GetPluginDefinition).mapTo[PluginDefinition].map(definition => (actor, definition))
    })
    Await.result(responses, Duration.Inf)
  }

  private def parseNotifications(definitions: List[(ActorRef, PluginDefinition)]) = {
    for ((actor, definition) <- definitions) {
      definition.notifications match {
        case Notifications(messages, rooms, participants) =>
          if (messages) {
            messageReceivers ::= actor
          }
          if (rooms) {
            roomReceivers ::= actor
          }
          if (participants) {
            participantReceivers ::= actor
          }
      }
    }
  }

  override def receive = {
    case CoreMessage(time, credential, text) => processMessage(time, credential, text)
    case CoreRoomJoin(time, roomJID, actor) => processRoomJoin(time, roomJID, actor)
    case CoreRoomLeave(time, roomJID) => processRoomLeave(time, roomJID)
    case CoreRoomTopicChanged(time, roomId, text, actor) => processRoomTopicChanged(time, roomId, text, actor)
    case CoreParticipantJoined(time, roomJID, participantJID, actor) => processParticipantJoin(time, roomJID, participantJID, actor)
    case CoreParticipantLeft(time, roomJID, participantJID, reason, actor) =>
      processParticipantLeave(time, roomJID, participantJID, reason, actor)
    case Core.GetPluginDefinitions => sender ! definitions
    case CoreGetCommands => sender ! Core.getCommandsDescription(definitions)
    case s: Subscribe =>
      subscribe(s)
      sender ! PositiveReply
    case Unsubscribe(actor) =>
      unsubscribe(actor)
      sender ! PositiveReply
    case EventMessage(event) => dispatchEvent(event)
  }

  private def processMessage(time: DateTime, credential: Credential, text: String) {
    val command = parseCommand(text)
    command match {
      case Some((name, arguments)) =>
        executeCommand(sender, credential, name, arguments)
      case None =>
    }

    for (plugin <- messageReceivers) {
      plugin ! ProcessMessage(time, credential, text)
    }
  }

  private def parseCommand(message: String): Option[(String, Array[String])] = {
    parsers.toStream.map(p =>
      p.parse(p.command, message) match {
        case p.Success((name, arguments), _) => Some(name.asInstanceOf[String] -> arguments.asInstanceOf[Array[String]])
        case _ => None
      }
    ).flatten.headOption
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

  private def processRoomJoin(time: DateTime, roomJID: String, actor: ActorRef) {
    for (plugin <- roomReceivers) {
      plugin ! ProcessRoomJoin(time, roomJID, actor)
    }
  }

  private def processRoomLeave(time: DateTime, roomJID: String) {
    for (plugin <- roomReceivers) {
      plugin ! ProcessRoomLeave(time, roomJID)
    }
  }

  private def processRoomTopicChanged(time: DateTime, roomId: String, text: String, roomActor: ActorRef) {
    for (plugin <- roomReceivers) {
      plugin ! ProcessRoomTopicChange(time, roomId, text)
    }
  }

  private def processParticipantJoin(time: DateTime, roomJID: String, participantJID: String, roomActor: ActorRef) {
    for (plugin <- participantReceivers) {
      plugin ! ProcessParticipantJoin(time, roomJID, participantJID, roomActor)
    }
  }

  private def processParticipantLeave(time: DateTime,
                                      roomJID: String,
                                      participantJID: String,
                                      reason: LeaveReason,
                                      roomActor: ActorRef) {
    for (plugin <- participantReceivers) {
      plugin ! ProcessParticipantLeave(time, roomJID, participantJID, reason, roomActor)
    }
  }

  private def subscribe(subscription: Subscribe) = {
    subscriptions ::= subscription
  }

  private def unsubscribe(actor: ActorRef) = {
    subscriptions = subscriptions.filterNot { sub => sub.actor.equals(actor) }
  }

  private def dispatchEvent(event: Event) = {
    subscriptions.filter {
      _.filter(event)
    } foreach {
      _.actor ! EventMessage(event)
    }
  }

}

object Core {

  /**
   * Asynchronously retrieve all plugin definitions from Core actor.
   *
   * @param core Core actor reference.
   * @return a list of initialized plugin definitions.
   */
  def getPluginDefinitions(core: ActorRef)(implicit timeout: Timeout): Future[List[(ActorRef, PluginDefinition)]] = {
    (core ? Core.GetPluginDefinitions).mapTo[List[(ActorRef, PluginDefinition)]]
  }

  private def getCommands(pluginDefinitions: List[(ActorRef, PluginDefinition)]): Map[String, List[(ActorRef, CommandDefinition)]] = {
    val commands = for ((actor, pluginDefinition) <- pluginDefinitions) yield {
      for (command <- pluginDefinition.commands) yield (command.name, actor, command)
    }

    val groups = commands.flatten.groupBy(_._1).map(tuple => (tuple._1, tuple._2.map {
      case (_, actor, command) => (actor, command)
    }))

    groups
  }

  private def getCommandsDescription(pluginDefinitions: List[(ActorRef, PluginDefinition)]) =
    pluginDefinitions.map(t => t._2.name -> t._2.commands.map(cd => cd.name -> cd.level)).toMap

  private def getStorages(pluginDefinitions: Seq[(ActorRef, PluginDefinition)]): Map[String, RepositoryFactory] = {
    pluginDefinitions.toStream.flatMap { case (_, definition) =>
      definition.repositoryFactory.map(factory => (definition.name, factory))
    }.toMap
  }

  private case object GetPluginDefinitions

}
