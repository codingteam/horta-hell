package ru.org.codingteam.horta.plugins.HelperPlugin

import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.messages.CoreGetCommands
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

private object ManCommand

class HelperPlugin extends CommandProcessor {
  override protected def name: String = "helper"

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
            Protocol.sendResponse(credential.location, credential, formatMan(commands, credential.access))

          case Failure(exception) =>
            log.error("Unable to get command list from core", exception)
            Protocol.sendResponse(credential.location, credential, s"Unable to get command list from core: ${exception.getMessage }")

        }
      case t => log.warning(s"Unknown command token passed to plugin $name: $t")
    }
  }

  def formatMan(commands: Map[String, List[(String, AccessLevel)]], level: AccessLevel): String = {
    val builder = StringBuilder.newBuilder
    builder.append(s"Available commands for your access level (${level.toString.replace("Access", "")}):\n")

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

  def formatCommand(command: String, index: Int, commandsTotal: Int) =
    s"$command" + (if (index != commandsTotal - 1) ", " else "")
}
