package ru.org.codingteam.horta.plugins.htmlreader

import ru.org.codingteam.horta.plugins.{CommandProcessor, CommandDefinition, BasePlugin}
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.protocol.Protocol
import org.jsoup.Jsoup

private object HtmlReaderCommand

class HtmlReaderPlugin() extends BasePlugin with CommandProcessor {

	override def name = "HtmlReader"

	/* TEXT CONSTANTS */
	private val commandName = "link"
	private val usageText = "Usage: $link [URL]"
	private val malformedUrl = "The url is malformed."
	private val httpResponseNotOK = "Cannot fetch the page."
	private val unknownHost = "The host is unknown."

	// current solution looks better
	// private val headerSize = 2000 // just random number, I hope most headers are less than 2000 characters

	override def commands = List(CommandDefinition(CommonAccess, commandName, HtmlReaderCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case HtmlReaderCommand =>
        try {
        	arguments match {
        		case Array(url, _*) => {
        		// current solution looks better
        		//	Protocol.sendResponse(credential.location, credential, Jsoup.parse(Source.fromURL(url).take(headerSize).mkString, "<html>", Parser.xmlParser()).select("title").iterator().next().ownText())
	        		val doc = Jsoup.connect(url).get()
							val title = doc.title()
							Protocol.sendResponse(credential.location, credential, title)
						}	
          	case _ =>
              Protocol.sendResponse(credential.location, credential, usageText)
          }
        } catch {
        	case e: java.net.MalformedURLException => {
        		Protocol.sendResponse(credential.location, credential, malformedUrl)
        	}
        	case e: org.jsoup.HttpStatusException => {
        		Protocol.sendResponse(credential.location, credential, httpResponseNotOK)
        	}
        	case e: java.net.UnknownHostException => {
        		Protocol.sendResponse(credential.location, credential, unknownHost)
        	}
        	case e: java.lang.IllegalArgumentException => {
        		Protocol.sendResponse(credential.location, credential, malformedUrl)
        	}
          case e: Exception => {
            e.printStackTrace()
            Protocol.sendResponse(credential.location, credential, "[ERROR] Something's wrong!")
          }
        }

      case _ => None
    }
  }
}