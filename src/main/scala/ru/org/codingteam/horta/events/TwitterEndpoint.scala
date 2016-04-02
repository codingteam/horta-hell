package ru.org.codingteam.horta.events

import java.util.concurrent.LinkedBlockingQueue

import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.UserstreamEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import ru.org.codingteam.horta.configuration.Configuration

/**
 * Created by hgn on 02.04.2016.
 */
class TwitterEndpoint(eventCollector: EventCollector) {

  val auth = new OAuth1(
    Configuration("twitter.auth.consumerKey"),
    Configuration("twitter.auth.consumerSecret"),
    Configuration("twitter.auth.token"),
    Configuration("twitter.auth.tokenSecret")
  )

  val queue = new LinkedBlockingQueue[String](1000)
  val client = new ClientBuilder()
    .authentication(auth)
    .hosts(Constants.STREAM_HOST)
    .endpoint(apiEndpoint)
    .processor(new StringDelimitedProcessor(queue)).build()
  private val apiEndpoint = new UserstreamEndpoint()

  def start(): Unit = {
    apiEndpoint.stallWarnings(false)
    apiEndpoint.withFollowings(true)

    client.connect()
  }

  def stop(): Unit = {
    client.stop()
  }
}
