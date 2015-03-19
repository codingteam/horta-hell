package ru.org.codingteam.horta.plugins.wtf

import akka.util.Timeout
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.{BasePlugin, CommandDefinition, CommandProcessor, DataAccessingPlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.Future
import scala.concurrent.duration._

private object WtfCommand
private object WtfDeleteCommand

class WtfPlugin extends BasePlugin with CommandProcessor with DataAccessingPlugin[WtfRepository] {
  import context.dispatcher

  implicit val timeout = Timeout(60.seconds)

  override def name: String = "wtf"

  override def commands = List(
    CommandDefinition(CommonAccess, "wtf", WtfCommand),
    CommandDefinition(CommonAccess, "wtf-delete", WtfDeleteCommand)
  )

  override protected val schema = "wtf"
  override protected val createRepository = WtfRepository.apply _

  protected def processCommand(credential: Credential,
                               token: Any,
                               arguments: Array[String]): Unit = token match {
    case WtfCommand => performWtfCommand(credential, arguments)
    case WtfDeleteCommand => performWtfDeleteCommand(credential, arguments)
    case _ => // nothing
  }

  private def performWtfCommand(credential: Credential, arguments: Array[String]): Unit =
    (credential.roomId, arguments) match {
      case (Some(room), Array(word)) => showDefinition(credential, room, word)
      case (Some(room), Array(word, definition)) => updateDefinition(credential, room, word, definition)
      case _ => sendResponse(credential, localize("Invalid arguments.")(credential))
    }

  private def performWtfDeleteCommand(credential: Credential, arguments: Array[String]): Unit = {
    (credential.roomId, arguments) match {
      case (Some(room), Array(word)) => deleteDefinition(credential, room, word)
      case _ => sendResponse(credential, localize("Invalid arguments.")(credential))
    }
  }

  private def showDefinition(credential: Credential, room: String, word: String): Unit = {
    withDatabase(_.read(room, word)) map {
      case Some(wtfDefinition: WtfDefinition) =>
        sendResponse(credential, s"> ${wtfDefinition.definition} © ${wtfDefinition.author}")
      case None =>
        sendResponse(credential, localize("Definition not found.")(credential))
    }
  }

  private def updateDefinition(credential: Credential, room: String, word: String, definition: String): Unit = {
    implicit val c = credential

    (word.trim, definition.trim) match {
      case ("", _) => sendResponse(credential, localize("You cannot define an empty string."))
      case (word, "") => deleteDefinition(credential, room, word)
      case (word, definition) => withDatabase(_.read(room, word)) map {
        case Some(wtfDefinition: WtfDefinition) => withDatabase(_.delete(wtfDefinition.id.get)) map {
          case true => withDatabase(_.store(WtfDefinition(None, room, word, definition, credential.name))) map { _ =>
            sendResponse(credential, localize("Definition updated."))
          }
          case false => sendResponse(credential, localize("Cannot update a definition."))
        }
        case None => withDatabase(_.store(WtfDefinition(None, room, word, definition, credential.name))) map { _ =>
          sendResponse(credential, localize("Definition added."))
        }
      }
    }
  }

  private def deleteDefinition(credential: Credential, room: String, word: String): Unit = {
    implicit val c = credential

    withDatabase(_.read(room, word)) map {
      case Some(wtfDefinition: WtfDefinition) => withDatabase(_.delete(wtfDefinition.id.get)) map {
        case true => sendResponse(credential, localize("Definition deleted."))
        case false => sendResponse(credential, localize("Cannot delete a definition."))
      }
      case None => sendResponse(credential, localize("Definition not found."))
    }
  }

  private def sendResponse(credential: Credential, message: String): Future[Boolean] =
    Protocol.sendResponse(credential.location, credential, message)
}
