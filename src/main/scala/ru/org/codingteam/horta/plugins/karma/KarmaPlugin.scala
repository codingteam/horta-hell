package ru.org.codingteam.horta.plugins.karma

import akka.util.Timeout
import org.joda.time.{DateTime, Period}
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.Localization
import ru.org.codingteam.horta.plugins.{BasePlugin, CommandDefinition, CommandProcessor, DataAccessingPlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.duration._

private object KarmaCommand
object KarmaAction extends Enumeration {
  type KarmaAction = String
  val KarmaChange = "change"
  val KarmaShow = "show"
  val KarmaTop = "top"
  val KarmaUp = "+"
  val KarmaDown = "-"
}
import ru.org.codingteam.horta.plugins.karma.KarmaAction._

class KarmaPlugin extends BasePlugin with CommandProcessor with DataAccessingPlugin[KarmaRepository] {

  val HELP_MESSAGE = s"karma $KarmaShow [username]\nkarma $KarmaTop\nkarma username $KarmaUp/$KarmaDown"

  val PERIOD_BETWEEN_CHANGES = 3 // hours

  override def name = "KarmaPlugin"

  override val schema = "Karma"
  override val createRepository = KarmaRepository.apply _

  implicit val timeout = Timeout(60.seconds)
  import context.dispatcher

  override def commands = List(
    CommandDefinition(CommonAccess, "karma", KarmaCommand)
  )

  override protected def processCommand(credential: Credential,
                                          token: Any,
                                          arguments: Array[String]): Unit = token match {
    case KarmaCommand => performKarmaCommand(credential, arguments)
    case _ => sendResponse(credential, Localization.localize("there is no such command")(credential))
  }

  def performKarmaCommand(credential: Credential, args: Array[String]): Unit = {
    val unknown = Localization.localize("unknown")(credential)
    args.toList match {
        case List(KarmaShow) =>
          showKarma(credential, credential.roomId.getOrElse(unknown), credential.name)
        case List(KarmaTop) =>
          showTopKarma(credential, credential.roomId.getOrElse(unknown))
        case List(KarmaShow, _) =>
          showKarma(credential, credential.roomId.getOrElse(unknown), args(1))
        case List(_, KarmaUp) =>
          changeKarma(credential, credential.roomId.getOrElse(unknown), args(0), 1)
        case List(_, KarmaDown) =>
          changeKarma(credential, credential.roomId.getOrElse(unknown), args(0), -1)
        case _ => sendResponse(credential, HELP_MESSAGE)
      }
  }

  private def sendResponse(credential: Credential, message: String): Unit =
    Protocol.sendResponse(credential.location, credential, message)

  private def showTopKarma(credential: Credential, room:String): Unit = {
    withDatabase(_.getTopKarma(room)) map { karma =>
      val msg = "\n" + karma.map(msg => msg).mkString("\n")
      sendResponse(credential, msg)
    }
  }

  private def showKarma(credential: Credential, room: String, user: String): Unit = {
    val template = Localization.localize("%s's karma")(credential)
    val text = template.format(user)
    withDatabase(_.getKarma(room, user)) map { karma =>
      sendResponse(credential, s"$text: $karma")
    }
  }

  private def changeKarma(credential: Credential, room:String, user: String, value: Int): Unit = {
    implicit val c = credential
    if (credential.name == user)
      sendResponse(credential, Localization.localize("You cannot change your karma."))
    else {
      withDatabase(_.getLastChangeTime(room, credential.name)) map { timeOption =>
        val canChangeCarma = timeOption match {
          case Some(time) =>
            new Period(time, Clock.now).toDurationFrom(DateTime.now).getStandardHours > PERIOD_BETWEEN_CHANGES
          case None =>
            true
        }
        if (canChangeCarma) {
          withDatabase(_.setKarma(credential.roomId.getOrElse("unknown"), credential.name, user, value)) map { _ =>
            val template = Localization.localize("%s's karma changed")
            sendResponse(credential, template.format(user))
          }
        } else {
          sendResponse(credential, Localization.localize("You cannot change karma too fast."))
        }
      }
    }
  }

}
