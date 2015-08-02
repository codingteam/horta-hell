package ru.org.codingteam.horta.plugins.loglist

import java.io.FileNotFoundException

import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.plugins.{BasePlugin, CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.security.{CommonAccess, Credential}
import ru.org.codingteam.horta.localization.Localization._

import scala.io.Source

import spray.json._

private object LogListCommand

class LogListPlugin extends BasePlugin with CommandProcessor {
  override def name = "loglist"

  override def commands = List(CommandDefinition(CommonAccess, "loglist", LogListCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) = {
    implicit val c = credential
    implicit val l = log
    val QuoteIdPattern = "([0-9]+)".r

    try {
      (token, arguments) match {
        case (LogListCommand, Array(QuoteIdPattern(id), _*)) => respondQuote(LogListApi.getQuoteById(id))
        case (LogListCommand, Array()) => respondQuote(LogListApi.getRandomQuote)
        case (LogListCommand, _) => respond("Usage: $loglist [quoteId]")

        case _ => // ignore
      }
    } catch {
      case e: FileNotFoundException => respond(localize("404 quote was not found"))
      case e: Exception => {
        respond(localize("[ERROR] Something bad happened. Please, do not panic and " +
          "tell Dr. von Never to take a look at the logs."))
        throw e
      }
    }
  }

  private def respondQuote(quote: Quote)(implicit credential: Credential) = quote match {
    case Quote(id, content, link) => respond(s"#$id\n$content\n\n$link")
  }

  private def respond(responseMessage: String)(implicit credential: Credential) = {
    Protocol.sendResponse(credential.location, credential, responseMessage)
  }
}
