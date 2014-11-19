package ru.org.codingteam.horta.plugins.karma


import akka.util.Timeout
import akka.pattern._
import org.joda.time.{Period, DateTime}
import ru.org.codingteam.horta.core.Clock
import scala.concurrent.duration._
import ru.org.codingteam.horta.database.{ReadObject, StoreObject, DAO}
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

private object KarmaCommand
private object KarmaChange {def name = "change"}
private object KarmaShow {def name = "show"}
private object KarmaTop {def name = "top"}
private object KarmaUp {def name = "+"}
private object KarmaDown {def name = "-"}

class KarmaPlugin extends BasePlugin with CommandProcessor {

  val HELP_MESSAGE = s"karma ${KarmaShow.name} [username]\nkarma ${KarmaTop.name}\nkarma username ${KarmaUp.name}/${KarmaDown.name}"

  val PERIOD_BETWEEN_CHANGES = 3 // hours

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
          showTopKarma(credential, credential.roomId.getOrElse("unknown"))
        case args if args.length == 2 && args(0) == KarmaShow.name =>
          showKarma(credential, credential.roomId.getOrElse("unknown"), args(1))
        case args if args.length == 2 && args(1) == KarmaUp.name =>
          changeKarma(credential, credential.roomId.getOrElse("unknown"), args(0), 1)
        case args if args.length == 2 && args(1) == KarmaDown.name =>
          changeKarma(credential, credential.roomId.getOrElse("unknown"), args(0), -1)
        case _ => sendResponse(credential, HELP_MESSAGE)
      }
  }

  private def sendResponse(credential: Credential, message: String): Unit =
    Protocol.sendResponse(credential.location, credential, message)

  private def showTopKarma(credential: Credential, room:String): Unit = {
    ((store ? ReadObject(name, GetTopKarma(room))) map {
      case Some(karma:Any) =>
        "\n" + karma.asInstanceOf[List[Any]].map(msg => msg).mkString("\n")
    }).onSuccess({case msg => sendResponse(credential,msg)})
  }

  private def showKarma(credential: Credential, room:String, user: String): Unit = {
    ((store ? ReadObject(name, GetKarma(room, user))) map {
      case Some(karma:Int) =>
        s"$user's karma: $karma"
      case _ =>
        s"$user's karma: 0"
    }).onSuccess({case msg => sendResponse(credential,msg)})
  }

  private def changeKarma(credential: Credential, room:String, user: String, value: Int): Unit = {
    if (credential.name == user)
      sendResponse(credential, "You cannot change your karma")
    else {
      ((store ? ReadObject(name, GetLastChangeTime(room, credential.name))) map {
        case Some(time:DateTime) =>
          new Period(time, Clock.now).toDurationFrom(DateTime.now).getStandardHours > PERIOD_BETWEEN_CHANGES
        case None =>
          true
      }).onSuccess({
        case canChangeCarma if canChangeCarma => {
          store ? StoreObject(name, Some(SetKarma(credential.roomId.getOrElse("unknown"), credential.name, user, value)), None)
          sendResponse(credential, s"$user's karma changed")
        }
        case _ =>
          sendResponse(credential, "You cannot change karma too fast")
      })
    }
  }
}
