package ru.org.codingteam.horta.plugins.wtf

import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.database.{DeleteObject, StoreObject, ReadObject}
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.concurrent.duration._
import scala.concurrent.Future

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
      case _ => sendResponse(credential, "Invalid arguments")
    }

  private def performWtfDeleteCommand(credential: Credential, arguments: Array[String]): Unit = {
    (credential.roomId, arguments) match {
      case (Some(room), Array(word)) => deleteDefinition(credential, room, word)
      case _ => sendResponse(credential, "Invalid arguments")
    }
  }

  private def showDefinition(credential: Credential, room: String, word: String): Unit = {
    store ? ReadObject(name, (room, word)) map {
      case Some(wtfDefinition: WtfDefinition) =>
        sendResponse(credential, s"> ${wtfDefinition.definition} © ${wtfDefinition.author}")
      case None =>
        sendResponse(credential, "Определение не найдено.")
    }
  }

  private def updateDefinition(credential: Credential, room: String, word: String, definition: String): Unit =
    (word.trim, definition.trim) match {
      case ("", _) => sendResponse(credential, "Нельзя определить пустую строку")
      case (word, "") => deleteDefinition(credential, room, word)
      case (word, definition) => store ? ReadObject(name, (room, word)) map {
        case Some(wtfDefinition: WtfDefinition) => store ? DeleteObject(name, wtfDefinition.id.get) map {
          case true => store ? StoreObject(name, None, WtfDefinition(None, room, word, definition, credential.name)) map {
            case Some(_) => sendResponse(credential, "Определение обновлено.")
            case None => sendResponse(credential, "Не удалось обновить определение.")
          }
          case false => sendResponse(credential, "Не удалось обновить определение.")
        }
        case None => store ? StoreObject(name, None, WtfDefinition(None, room, word, definition, credential.name)) map {
          case Some(_) => sendResponse(credential, "Определение добавлено.")
          case None => sendResponse(credential, "Не удалось добавить определение.")
        }
    }
  }

  private def deleteDefinition(credential: Credential, room: String, word: String): Unit = {
    store ? ReadObject(name, (room, word)) map {
      case Some(wtfDefinition: WtfDefinition) => store ? DeleteObject(name, wtfDefinition.id.get) map {
        case true => sendResponse(credential, "Определение удалено.")
        case false => sendResponse(credential, "Не удалось удалить определение.")
      }
      case None => sendResponse(credential, "Определение не найдено.")
    }
  }

  private def sendResponse(credential: Credential, message: String): Future[Boolean] =
    Protocol.sendResponse(credential.location, credential, message)
}
