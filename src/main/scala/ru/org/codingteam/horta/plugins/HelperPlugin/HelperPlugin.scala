package ru.org.codingteam.horta.plugins.HelperPlugin

import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.messages.CoreGetCommands
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ManCommand

class HelperPlugin extends CommandProcessor {
  implicit val timeout = Timeout(60 seconds)

  override def commands = List(
    CommandDefinition(CommonAccess, "man", ManCommand),
    CommandDefinition(CommonAccess, "help", ManCommand)
  )

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case ManCommand =>
        (core ? CoreGetCommands).mapTo[Map[String, List[(String, AccessLevel)]]].onComplete {
          case Success(commands) =>
            arguments match {
              case Array() => Protocol.sendResponse(credential.location, credential, formatMan(commands, credential))
              case Array(command) => Protocol.sendResponse(credential.location, credential, commandMan(credential, command))
              case _ => Protocol.sendResponse(credential.location, credential,
                errorResponse(credential))
            }

          case Failure(exception) =>
            log.error("Unable to get command list from core", exception)
            val message = Localization.localize("Unable to get command list from core")(credential)
            Protocol.sendResponse(credential.location, credential, s"$message: ${exception.getMessage}")

        }
      case t => log.warning(s"Unknown command token passed to plugin $name: $t")
    }
  }

  override protected def name: String = "helper"

  def errorResponse(credential: Credential): String = {
    implicit val c = credential
    Localization.localize("I'm sorry, %s. I'm afraid I can't help you with that.").format(credential.name)
  }

  def commandMan(credential: Credential, command: String): String = {
    implicit val c = credential
    Localization.localizeOpt("command.help."+command) getOrElse { errorResponse(credential) }
  }

  def formatMan(commands: Map[String, List[(String, AccessLevel)]], credential: Credential): String = {
    implicit val c = credential
    val level = credential.access

    val builder = StringBuilder.newBuilder

    val text = Localization.localize("Available commands for your access level")
    val levelString = Localization.localize(level.toString)
    builder.append(s"$text ($levelString):\n")

    val len = commands.maxBy(_._1.length)._1.length

    commands.foreach { case (plugin, commandList) =>
      builder.append(formatPlugin(plugin, len))

      builder.append(
        (level match {
          case CommonAccess => commandList.filter(_._2 == CommonAccess)
          case RoomAdminAccess => commandList.filter(c => c._2 == CommonAccess || c._2 == RoomAdminAccess)
          case GlobalAccess => commandList
        }).map { _._1 }.mkString(", ")
      )

      builder.append("\n")
    }

    builder.mkString
  }

  def formatPlugin(plugin: String, maxNameLength: Int) =
    s"• ${plugin.take(1).toUpperCase + plugin.substring(1)}: " + "." * (maxNameLength + 4 - plugin.length) + " "
}
