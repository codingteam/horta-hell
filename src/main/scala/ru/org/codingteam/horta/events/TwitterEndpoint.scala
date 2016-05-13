package ru.org.codingteam.horta.events

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import akka.event.slf4j.Logger
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.UserstreamEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.messages.TwitterEvent
import spray.json.DefaultJsonProtocol._
import spray.json._

class TwitterEndpoint extends EventEndpoint {

  private lazy val consumerKey = Configuration("twitter.auth.consumerKey")
  private lazy val consumerSecret = Configuration("twitter.auth.consumerSecret")
  private lazy val authToken = Configuration("twitter.auth.token")
  private lazy val tokenSecret = Configuration("twitter.auth.tokenSecret")
  private lazy val auth = new OAuth1(consumerKey, consumerSecret, authToken, tokenSecret)
  private lazy val client = new ClientBuilder()
    .authentication(auth)
    .hosts(Constants.USERSTREAM_HOST)
    .endpoint(apiEndpoint)
    .processor(new StringDelimitedProcessor(queue))
    .build()
  private val log = Logger(classOf[TwitterEndpoint], "TwitterEndpoint")
  private val queue = new LinkedBlockingQueue[String](1000)
  private val apiEndpoint = new UserstreamEndpoint()

  override def start(): Unit = {
    log.info("Trying to connect to Twitter backend")

    apiEndpoint.stallWarnings(false)
    apiEndpoint.withFollowings(true)

    client.connect()

    log.info("Connected to Twitter backend")
  }

  override def stop(): Unit = {
    log.info("Disconnecting from Twitter stream")
    client.stop() // after this client.isDone() should return true
  }

  override def process(eventCollector: EventCollector): Unit = {
    while (!client.isDone) {
      log.trace("Polling twitter queue...")
      Option(queue.poll(1, TimeUnit.SECONDS)) match {
        case Some(message) => processMessage(eventCollector, message)
        case None => log.debug("Timed out waiting for twitter message.")
      }
      log.trace("Queue polling finished")
    }
    log.info("Client has closed connection.")
  }

  private def processMessage(eventCollector: EventCollector, message: String): Unit = {
    log.debug("Message from twitter stream: {}", message)
    val jsonMessage = message.parseJson.asJsObject
    val author = jsonMessage.fields.get("user") flatMap {
      _.asJsObject.fields.get("screen_name")
    } map {
      _.convertTo[String]
    }
    val tweet = jsonMessage.fields.get("text") map {
      _.convertTo[String]
    }

    (author, tweet) match {
      case (Some(authorStr: String), Some(tweetStr: String)) =>
        log.debug("Got a message : ()", (authorStr, tweetStr))
        eventCollector.onEvent(TwitterEvent(authorStr, tweetStr))
      case _ => log.trace("Not a tweet: {}", message)
    }
  }

  override def validate(): Boolean = {
    try {
      (Configuration("twitter.enabled").toBoolean, consumerKey, consumerSecret, authToken, tokenSecret) match {
        case (false, _, _, _, _) =>
          log.info("Twitter integration is disabled")
          false
        case (_, null, _, _, _) | (_, _, null, _, _) | (_, _, _, null, _) | (_, _, _, _, null) =>
          log.error("Incomplete configuration.")
          false
        case _ => true
      }
    } catch {
      case e: IllegalArgumentException =>
        log.error("Exception while validating an endpoint", e)
        false
    }
  }
}
