package ru.org.codingteam.horta.plugins.wtf

import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.database.{DeleteObject, ReadObject, StoreObject}
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.plugins.{BasePlugin, CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent.Future
import scala.concurrent.duration._

private object WtfCommand
private object WtfDeleteCommand

class WtfPlugin extends BasePlugin with CommandProcessor {
  import context.dispatcher

  implicit val timeout = Timeout(60.seconds)

  override def name: String = "wtf"

  override def commands = List(
    CommandDefinition(CommonAccess, "wtf", WtfCommand),
    CommandDefinition(CommonAccess, "wtf-delete", WtfDeleteCommand)
  )

  override def dao = Some(new WtfDAO)

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
    store ? ReadObject(name, (room, word)) map {
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
      case (word, definition) => store ? ReadObject(name, (room, word)) map {
        case Some(wtfDefinition: WtfDefinition) => store ? DeleteObject(name, wtfDefinition.id.get) map {
          case true => store ? StoreObject(name, None, WtfDefinition(None, room, word, definition, credential.name)) map {
            case Some(_) => sendResponse(credential, localize("Definition updated."))
            case None => sendResponse(credential, localize("Cannot update a definition."))
          }
          case false => sendResponse(credential, localize("Cannot update a definition."))
        }
        case None => store ? StoreObject(name, None, WtfDefinition(None, room, word, definition, credential.name)) map {
          case Some(_) => sendResponse(credential, localize("Definition added."))
          case None => sendResponse(credential, localize("Cannot add a definition."))
        }
      }
    }
  }

  private def deleteDefinition(credential: Credential, room: String, word: String): Unit = {
    implicit val c = credential

    store ? ReadObject(name, (room, word)) map {
      case Some(wtfDefinition: WtfDefinition) => store ? DeleteObject(name, wtfDefinition.id.get) map {
        case true => sendResponse(credential, localize("Definition deleted."))
        case false => sendResponse(credential, localize("Cannot delete a definition."))
      }
      case None => sendResponse(credential, localize("Definition not found."))
    }
  }

  private def sendResponse(credential: Credential, message: String): Future[Boolean] =
    Protocol.sendResponse(credential.location, credential, message)
}
