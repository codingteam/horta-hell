package ru.org.codingteam.horta.actors.core

import akka.actor.{Props, ActorLogging, Actor}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.actors.Messenger
import ru.org.codingteam.horta.security._
import ru.org.codingteam.horta.messages.InitializePlugin
import ru.org.codingteam.horta.security.KnownUser
import ru.org.codingteam.horta.security.BotOwner
import ru.org.codingteam.horta.messages.ExecuteCommand
import scala.Some
import ru.org.codingteam.horta.security.UnknownUser

class Core extends Actor with ActorLogging {
  val plugins = {
    val messenger = context.actorOf(Props[Messenger](), "messenger")
    Map("messenger" -> messenger)
  }

  for (plugin <- plugins.values) {
    plugin ! InitializePlugin(context.self, plugins)
  }

  var commands = Map[String, Command]()

  def receive = {
    case RegisterCommand(command, role, receiver) => {
      commands = commands.updated(command, new Command(command, role, receiver))
    }

    case ProcessCommand(user, message) => {
      val arguments = parseCommand(message)
      arguments match {
        case Some(CommandArguments(Command(name, role, target), args)) => {
          if (accessGranted(user, role)) {
            target ! ExecuteCommand(user, name, args)
          }
        }

        case None =>
      }
    }
  }

  def accessGranted(user: User, role: UserRole) = {
    role match {
      case BotOwner()    => user.role == BotOwner()
      case KnownUser()   => user.role == BotOwner() || user.role == KnownUser()
      case UnknownUser() => user.role == BotOwner() || user.role == KnownUser() || user.role == UnknownUser()
    }
  }

  val commandNameRegex = "^\\$([^\\s]+)".r

  def parseCommand(message: String) = {
    message match {
      case commandNameRegex(command) => {
        commands.get(command) match {
          case Some(command) => Some(CommandArguments(command, parseArguments(message)))
          case None          => None
        }
      }
      case _ => None
    }
  }

  def parseArguments(message: String) = {
    // TODO: Advanced parser with quote syntax
    message.split(' ').tail
  }
}
