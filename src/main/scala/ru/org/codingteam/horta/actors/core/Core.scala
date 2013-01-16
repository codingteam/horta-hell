package ru.org.codingteam.horta.actors.core

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.actors.Messenger
import ru.org.codingteam.horta.security._
import scala.Some
import scala.util.parsing.combinator._
import scala.util.matching.Regex

class Core extends Actor with ActorLogging {
  var commands = Map[String, Command]()
  var plugins: Map[String, ActorRef] = null

  override def preStart() = {
    val messenger = context.actorOf(Props(new Messenger(self)), "messenger")
    plugins = Map("messenger" -> messenger)
  }

  def receive = {
    case RegisterCommand(mode, command, role, receiver) => {
      commands = commands.updated(command, Command(mode, command, role, receiver))
    }

    case ProcessCommand(user, message) => {
      val arguments = parseCommand(message)
      arguments match {
        case Some(CommandArguments(Command(mode, name, role, target), args)) => {
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
      case BotOwner    => user.role == BotOwner
      case KnownUser   => user.role == BotOwner || user.role == KnownUser
      case UnknownUser => user.role == BotOwner || user.role == KnownUser || user.role == UnknownUser
    }
  }

  val dollarCommandNameRegex = "^\\$([^\\s]+).*?$".r
  val slashCommandNameRegex = "^([^\\s]+?)/.*?$".r

  def parseCommand(message: String) = {
    message match {
      case dollarCommandNameRegex(command) => {
        commands.get(command) match {
          case Some(command) => Some(CommandArguments(command, parseDollarArguments(message)))
          case None          => None
        }
      }

      case slashCommandNameRegex(command) => {
        commands.get(command) match {
          case Some(command) => Some(CommandArguments(command, parseSlashArguments(message)))
          case None          => None
        }
      }

      case _ => None
    }
  }

  def parseDollarArguments(message: String) : Array[String] = {
    val parser = new RegexParsers {
      override type Elem = Char
      def command_name = regexMatch("^(\\$[^\\s]+)".r) ^^ {m => m.group(1)}
      def regular_arg = regexMatch("([^\\s]+)".r) ^^ {m => m.group(1)}
      def quoted_arg = regexMatch("\"(.*?(?<!\\\\)(\\\\\\\\)*)\"".r) ^^ {m => m.group(1)}
      def command = command_name ~ ((quoted_arg | regular_arg) *) ^^ {case name~arguments => arguments}

      /* http://stackoverflow.com/questions/1815716/accessing-scala-parser-regular-expression-match-data */
      def regexMatch(r: Regex): Parser[Regex.Match] = new Parser[Regex.Match] {
        def apply(in: Input) = {
          val source = in.source
          val offset = in.offset
          val start = handleWhiteSpace(source, offset)
          (r findPrefixMatchOf (source.subSequence(start, source.length))) match {
            case Some(matched) =>
              Success(matched, in.drop(start + matched.end - offset))
            case None =>
              Failure("string matching regex `"+r+"' expected but `"+in.first+"' found", in.drop(start - offset))
          }
        }
      }
    }
    
    parser.parse(parser.command, message) match {
      case parser.Success(arguments, _) => (arguments map {x => x.replace("\\\\", "\\").replace("\\\"", "\"")}).toArray
      case _ => Array()
    }
  }

  def parseSlashArguments(message: String) = {
    message.split("/", -1).tail
  }
}
