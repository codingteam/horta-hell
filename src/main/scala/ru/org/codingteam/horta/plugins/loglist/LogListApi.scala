package ru.org.codingteam.horta.plugins.loglist

import akka.event.LoggingAdapter
import ru.org.codingteam.horta.configuration.Configuration

import scala.io.{Codec, Source}
import spray.json._
import spray.json.DefaultJsonProtocol._

object LogListApi {
  private lazy val LOGLIST_QUOTE_API_BASE_URL = s"${Configuration.loglistUrl}/api/quote"
  private lazy val LOGLIST_QUOTE_BASE_URL = s"${Configuration.loglistUrl}/quote"

  def getRandomQuote(implicit log: LoggingAdapter): Quote = {
    retrieveQuoteFromUrl(s"$LOGLIST_QUOTE_API_BASE_URL/random")
  }

  def getQuoteById(id: String)(implicit log: LoggingAdapter): Quote = {
    retrieveQuoteFromUrl(s"$LOGLIST_QUOTE_API_BASE_URL/$id")
  }

  private def retrieveQuoteFromUrl(url: String)(implicit log: LoggingAdapter): Quote = {
    log.info(s"Requesting $url")
    val json = Source.fromURL(url)(Codec.UTF8).mkString
    log.info(s"Json answer: $json")

    json.parseJson.asJsObject.getFields("id", "content") match {
      case Vector(JsString(id), JsString(content)) => Quote(id, content, s"$LOGLIST_QUOTE_BASE_URL/$id")
    }
  }
}
