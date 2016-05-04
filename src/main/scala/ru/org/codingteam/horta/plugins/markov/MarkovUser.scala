package ru.org.codingteam.horta.plugins.markov

import java.util.Locale

import akka.actor.{Stash, Actor, ActorLogging}
import akka.util.Timeout
import me.fornever.platonus.Network
import org.joda.time.{DateTime, Period}
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.localization.Localization._
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.Credential

import scala.concurrent.duration._
import scala.language.postfixOps

class MarkovUser(val room: String, val nick: String) extends Actor with ActorLogging with Stash {

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  object Tick

  val cacheTime = 5 minutes
  val seriesTime = 1 minute

  val plugin = context.parent

  var network: Option[Network] = None
  var firstSeriesMessageTime: Option[DateTime] = None
  var seriesMessages = 0
  var lastMessage: Option[String] = None // TODO: It relies on a permanent message storage. Move this to another plugin.
                                         // TODO: Currently there will be problems when a user gets disposed and
                                         // TODO: restored (his last message will be forgotten by Horta).
  var lastNetworkTime: Option[DateTime] = None

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
      if (ensureInitialized()) {
        val location = credential.location

        def generator() = {
          val phrase = generatePhrase(network.get, length)(credential)
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
      }

    case ReplaceRequest(credential, from, to) =>
      val location = credential.location

      lastMessage match {
        case Some(message) =>
          val newMessage = message.replace(from, to)
          if (newMessage != message) {
              lastMessage = Some(newMessage)
              Protocol.sendResponse(location, credential, newMessage)
          }
        case None =>
          Protocol.sendResponse(location, credential, localize("No messages for you, sorry.")(credential))
      }

    case Tick =>
      // Analyse and flush cache on tick.
      lastNetworkTime match {
        case Some(time) =>
          val period = new Period(time, Clock.now)
          if (period.getMillis > cacheTime.toMillis) {
            MarkovPlugin.disposeUser(plugin, UserIdentity(room, nick))
          }

        case None =>
      }
  }

  def ensureInitialized(): Boolean = {
    network match {
      case Some(_) =>
        lastNetworkTime = Some(Clock.now)
        true

      case None =>
        log.debug(s"Initializing cache for user $nick")

        context.become {
          case newNetwork: Network =>
            log.debug(s"Received network for user $nick")
            network = Some(newNetwork)
            lastNetworkTime = Some(Clock.now)
            context.unbecome()
            unstashAll()
          case _ =>
            stash()
        }

        MarkovPlugin.parseLogs(plugin, UserIdentity(room, nick)).onSuccess({
          case n => self ! n
        })

        stash()

        false
    }
  }

  def flushNetwork() {
    if (network.isDefined) {
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
      val phrase = network.generate(Configuration.markovMessageWordLimit)
      if (phrase.length >= length) {
        return phrase.mkString(" ")
      }
    }

    localize("Requested phrase was not found, sorry.")
  }
}
