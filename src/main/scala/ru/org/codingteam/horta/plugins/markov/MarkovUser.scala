package ru.org.codingteam.horta.plugins.markov

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import java.util.{Calendar, Locale}
import me.fornever.platonus.Network
import org.joda.time.DateTime
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.localization.Localization._

class MarkovUser(val room: String, val nick: String) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  object Tick

  val cacheTime = 5 minutes
  val seriesTime = 1 minute

  var network: Option[Network] = None
  var firstSeriesMessageTime: Option[DateTime] = None
  var seriesMessages = 0
  var lastMessage: Option[String] = None
  var lastNetworkTime: Option[Calendar] = None

  override def preStart() {
    context.system.scheduler.schedule(cacheTime, cacheTime, self, Tick)
  }

  def receive = {
    case UserPhrase(message) =>
      if (addPhrase(message)) {
        lastMessage = Some(message)
      }

    case AddPhrase(phrase) =>
      addPhrase(phrase)

    case GeneratePhrase(credential, length, bloodMode) =>
      val location = credential.location
      val network = getNetwork()

      def generator() = {
        val phrase = generatePhrase(network, length)(credential)
        if (bloodMode) phrase.toUpperCase(Locale.ROOT) else phrase
      }

      val result = if (bloodMode) {
        (generator(), false)
      } else {
        val currentTime = Clock.now
        def resetSeries() = {
          firstSeriesMessageTime = Some(currentTime)
          seriesMessages = 1
          (generator(), false)
        }

        firstSeriesMessageTime match {
          case Some(time) =>
            val inSeries = time.plusMillis(seriesTime.toMillis.toInt).isAfter(currentTime)
            if (inSeries && seriesMessages < Configuration.markovMessagesPerMinute) {
              seriesMessages += 1
              (generator(), false)
            } else if (!inSeries) {
              resetSeries()
            } else {
              (generator(), true)
            }

          case _ =>
            resetSeries()
        }
      }

      result match {
        case (message, false) =>
          Protocol.sendResponse(location, credential, message)
        case (message, true) =>
          Protocol.sendPrivateResponse(location, credential, message)
      }

    case ReplaceRequest(credential, from, to) =>
      val location = credential.location

      lastMessage match {
        case Some(message) =>
          val newMessage = message.replace(from, to)
          lastMessage = Some(newMessage)
          Protocol.sendResponse(location, credential, newMessage)
        case None =>
          Protocol.sendResponse(location, credential, localize("No messages for you, sorry.")(credential))
      }

    case Tick =>
      // Analyse and flush cache on tick.
      lastNetworkTime match {
        case Some(time) =>
          val msDiff = Calendar.getInstance.getTimeInMillis - time.getTimeInMillis
          if (msDiff > cacheTime.toMillis) {
            flushNetwork()
          }

        case None =>
      }
  }

  def getNetwork(): Network = {
    network match {
      case Some(network) =>
        lastNetworkTime = Some(Calendar.getInstance)
        network

      case None =>
        // TODO: Get logs from the database, *block on the log getter with something like `become`*.
        val parsedNetwork = LogParser.parse(log, room, nick)
        network = Some(parsedNetwork)
        lastNetworkTime = Some(Calendar.getInstance)
        parsedNetwork
    }
  }

  def flushNetwork() {
    if (!network.isEmpty) {
      log.info(s"Flushing Markov network cache of user $nick")
      network = None
    }
  }

  def addPhrase(phrase: String) = {
    if (!phrase.startsWith("$") && !phrase.startsWith("s/")) {
      network match {
        case Some(network) => network.add(LogParser.tokenize(phrase))
        case None => // will add later on log parse
      }
      true
    } else {
      false
    }
  }

  def generatePhrase(network: Network, length: Integer)(implicit credential: Credential): String = {
    for (i <- 1 to 25) {
      val phrase = network.generate()
      if (phrase.length >= length) {
        return phrase.mkString(" ")
      }
    }

    localize("Requested phrase was not found, sorry.")
  }
}
