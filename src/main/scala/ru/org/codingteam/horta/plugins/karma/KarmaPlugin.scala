package ru.org.codingteam.horta.plugins.karma


import akka.util.Timeout
import akka.pattern._
import scala.concurrent.duration._
import ru.org.codingteam.horta.database.{ReadObject, StoreObject, DAO}
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

private object KarmaCommand
private object KarmaChange {def name = "change"}
private object KarmaShow {def name = "show"}
private object KarmaTop {def name = "top"}
private object KarmaBottom {def name = "bottom"}

class KarmaPlugin extends BasePlugin with CommandProcessor {

  val HELP_MESSAGE = "$karma show [username]\n$karma top\n$karma username change"

  override def name = "KarmaPlugin"

  override protected def dao: Option[DAO] = Some(new KarmaDAO())

  implicit val timeout = Timeout(60.seconds)
  import context.dispatcher

  override def commands = List(
    CommandDefinition(CommonAccess, "karma", KarmaCommand)
  )

  /**
   * Process a command.
   * @param credential a credential of a user executing the command.
   * @param token token registered for command.
   * @param arguments command argument array.
   * @return string for replying the sender.
   */
  override protected def processCommand(credential: Credential,
                               token: Any,
                               arguments: Array[String]): Unit = token match {
    case KarmaCommand => performKarmaCommand(credential, arguments)
    case _ => sendResponse(credential, "")
  }

  def performKarmaCommand(credential: Credential, args: Array[String]): Unit = {
    args match {
        case args if args.length == 1 && args(0) == KarmaShow.name =>
          showKarma(credential, credential.roomId.getOrElse("unknown"), credential.name)
        case args if args.length == 1 && args(0) == KarmaTop.name =>
          showTopKarma(credential, credential.roomId.getOrElse("unknown"), AscendingOrder())
        case args if args.length == 1 && args(0) == KarmaBottom.name =>
          showTopKarma(credential, credential.roomId.getOrElse("unknown"), DescendingOrder())
        case args if args.length == 2 && args(0) == KarmaShow.name =>
          showKarma(credential, credential.roomId.getOrElse("unknown"), args(1))
        case args if args.length == 2 =>
          changeKarma(credential, args(0), args(1))
        case _ => sendResponse(credential, HELP_MESSAGE)
      }
  }

  private def sendResponse(credential: Credential, message: String): Unit =
    Protocol.sendResponse(credential.location, credential, message)

  private def showTopKarma(credential: Credential, room:String, order: Order): Unit = {
    ((store ? ReadObject(name, GetTopKarma(room, order))) map {
      case Some(karma:List[Int]) =>
        karma.map(msg => msg).mkString("\n")
    }).onSuccess({case msg => sendResponse(credential,msg)})
  }

  private def showKarma(credential: Credential, room:String, user: String): Unit = {
    ((store ? ReadObject(name, GetKarma(room, user))) map {
      case Some(karma:Int) =>
        s"$user's karma: $karma"
    }).onSuccess({case msg => sendResponse(credential,msg)})
  }

  private def changeKarma(credential: Credential, user: String, value: String): Unit = {
    val msg = if (credential.name != name) {
      store ? StoreObject(name, None, SetKarma(credential.roomId.getOrElse("unknown"), user, value.toInt))
      s"$user's karma changed"
    } else
      "You cannot change your karma"
    sendResponse(credential,msg)
  }
}
