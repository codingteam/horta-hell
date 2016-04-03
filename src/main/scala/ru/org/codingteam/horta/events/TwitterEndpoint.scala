package ru.org.codingteam.horta.events

import java.util.concurrent.LinkedBlockingQueue

import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.UserstreamEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import ru.org.codingteam.horta.configuration.Configuration

class TwitterEndpoint extends EventEndpoint {

  val auth = new OAuth1(
    Configuration("twitter.auth.consumerKey"),
    Configuration("twitter.auth.consumerSecret"),
    Configuration("twitter.auth.token"),
    Configuration("twitter.auth.tokenSecret")
  )

  private val queue = new LinkedBlockingQueue[String](1000)
  private val apiEndpoint = new UserstreamEndpoint()
  private val client = new ClientBuilder()
    .authentication(auth)
    .hosts(Constants.STREAM_HOST)
    .endpoint(apiEndpoint)
    .processor(new StringDelimitedProcessor(queue))
    .build()

  override def start(): Unit = {
    apiEndpoint.stallWarnings(false)
    apiEndpoint.withFollowings(true)

    client.connect()
  }

  override def stop(): Unit = {
    client.stop() // after this client.isDone() should return true
  }

  override def process(eventCollector: EventCollector): Unit = {
    while (!client.isDone) {
      val message = queue.take()
      //TODO: Parse JSON and invoke onEvent callback
    }
  }
}
