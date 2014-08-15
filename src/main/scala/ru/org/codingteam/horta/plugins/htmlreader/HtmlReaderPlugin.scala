package ru.org.codingteam.horta.plugins.htmlreader

import java.net.{HttpURLConnection, URL}

import org.jsoup.Jsoup
import ru.org.codingteam.horta.plugins.{BasePlugin, CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.io.Source

private object HtmlReaderCommand

class HtmlReaderPlugin() extends BasePlugin with CommandProcessor {

  override def name = "HtmlReader"

  private val commandName = "link"
  private val usageText = "Usage: $link [URL]"
  private val malformedUrl = "The url is malformed."
  private val httpResponseNotOK = "Cannot fetch the page"
  private val unknownHost = "The host is unknown."

  private val headerSize = 2000 // just random number, I hope most headers are less than 2000 characters

  override def commands = List(CommandDefinition(CommonAccess, commandName, HtmlReaderCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case HtmlReaderCommand =>
        val responseText = new StringBuilder()
        try {
          arguments match {
            case Array(address) =>
              val url = new URL(address)
              url.getProtocol match {
                case "http" | "https" =>
                  val connection = url.openConnection().asInstanceOf[HttpURLConnection]
                  connection.setRequestMethod("GET")
                  connection.connect()

                  val code = connection.getResponseCode
                  responseText.append("HTTP: ").append(code).append(", ")

                  val doc = Jsoup.parse(Source.fromURL(url).take(headerSize).mkString)
                  val title = doc.title()
                  responseText.append(title)
                case _ => throw new java.net.MalformedURLException()
              }
            case _ =>
              Protocol.sendResponse(credential.location, credential, usageText)
          }
        } catch {
          case e@(_: java.net.MalformedURLException | _: java.lang.IllegalArgumentException) =>
            responseText.append(malformedUrl)
          case e: java.net.UnknownHostException =>
            responseText.append(unknownHost)
          case e@(_: org.jsoup.HttpStatusException | _: java.io.IOException) =>
            log.error(e, "HTTP exception")
            responseText.append(httpResponseNotOK)
          case e: Exception =>
            log.error(e, e.toString)
            Protocol.sendResponse(credential.location, credential, "[ERROR] Something's wrong!")
        }
        if (responseText.nonEmpty) {
          Protocol.sendResponse(credential.location, credential, responseText.toString())
        }

      case _ => None
    }
  }
}