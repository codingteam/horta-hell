package ru.org.codingteam.horta.plugins

import akka.actor.{Actor, ActorLogging}
import ru.org.codingteam.horta.database.DAO

/**
 * Common plugin functionality. Every plugin should be inherited from this class.
 */
abstract class BasePlugin extends Actor with ActorLogging {

  def receive = {
    case GetPluginDefinition => sender ! pluginDefinition
  }

  protected val core = context.actorSelection("/user/core")

  /**
   * Plugin name.
   * @return unique plugin name.
   */
  protected def name: String

  /**
   * Plugin notification sources.
   * @return object containing definition of notification sources.
   */
  protected def notifications = Notifications(messages = false, rooms = false, participants = false)

  /**
   * A collection of command definitions.
   * @return a collection.
   */
  protected def commands: List[CommandDefinition] = List()

  /**
   * A full plugin definition.
   * @return plugin definition.
   */
  protected def pluginDefinition = PluginDefinition(name, notifications, commands, None)

}
