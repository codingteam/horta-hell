package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.io.Source
import scala.util.parsing.json._

private object FortuneCommand

class FortunePlugin extends BasePlugin with CommandProcessor {

  private val maxLength = 128

  override def name = "fortune"

  override def commands = List(CommandDefinition(CommonAccess, "fortune", FortuneCommand))

  private def parseResponse(rawText: String): String = {
    val json = JSON.parseFull(rawText)
    val response = json.get.asInstanceOf[Map[String, Any]]
    val status = response.get("status").map(_.asInstanceOf[String])
    status match {
      case Some("ok") => {
        val body = response.get("body").map(_.asInstanceOf[String])
        val id = response.get("id").map(_.asInstanceOf[Double])
        (id, body) match {
          case (Some(id), Some(body)) => s"#${id.toInt}\n$body"
          case _ => "Wrong response from the service"
        }
      }
      case Some("not_found") => "The fortune was not found."
      case _ => "Wrong response from the service"
    }
  }

  private def getFortuneByUrl(credential: Credential, url: String) = {
    val rawText = Source.fromURL(url).mkString
    Protocol.sendResponse(credential.location, credential, parseResponse(rawText))
  }

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case FortuneCommand =>
        try {
          val FortuneIdPattern = "([0-9]+)".r
          arguments match {
            case Array(FortuneIdPattern(fortuneId), _*) =>
              getFortuneByUrl(credential, s"http://rexim.me/api/$fortuneId?max_length=$maxLength")

            case Array() =>
              getFortuneByUrl(credential, s"http://rexim.me/api/random?max_length=$maxLength")

            case _ =>
              Protocol.sendResponse(credential.location, credential, "Usage: $fortune [fortune-id:number]")
          }
        } catch {
          case e: Exception => {
            e.printStackTrace()
            Protocol.sendResponse(credential.location, credential, "[ERROR] Something's wrong!")
          }
        }

      case _ => None
    }
  }

}
