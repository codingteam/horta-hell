package ru.org.codingteam.horta.plugins.markov

import akka.actor.{PoisonPill, Props, ActorLogging, Actor}
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime
import platonus.Network
import ru.org.codingteam.horta.messages._
import scala.concurrent.duration._
import scala.language.postfixOps
import java.util.{Calendar, Locale}
import scala.concurrent.Future
import ru.org.codingteam.horta.configuration.Configuration

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
    case SetNetwork(newNetwork) =>
      network = Some(newNetwork)
      lastNetworkTime = Some(Calendar.getInstance)

    case UserPhrase(message) =>
      if (addPhrase(message)) {
        lastMessage = Some(message)
      }

    case AddPhrase(phrase) =>
      addPhrase(phrase)

    case GeneratePhrase(credential, length, bloodMode) =>
      val location = credential.location

      getNetwork() map {
        case network =>
          def generator() = {
            val phrase = generatePhrase(network, length)
            if (bloodMode) phrase.toUpperCase(Locale.ROOT) else phrase
          }

          val result = if (bloodMode) {
            generator()
          } else {
            val currentTime = DateTime.now
            def resetSeries() = {
              firstSeriesMessageTime = Some(currentTime)
              seriesMessages = 1
              generator()
            }

            firstSeriesMessageTime match {
              case Some(time) =>
                val inSeries = time.plusMillis(seriesTime.toMillis.toInt).isAfter(currentTime)
                if (inSeries && seriesMessages < Configuration.markovMessagesPerMinute) {
                  seriesMessages += 1
                  generator()
                } else if (!inSeries) {
                  resetSeries()
                } else {
                  s"Message count exceeded, wait for $seriesTime"
                }

              case _ =>
                resetSeries()
            }
          }

          location ! SendResponse(credential, result)
      }

    case ReplaceRequest(credential, from, to) =>
      val location = credential.location

      lastMessage match {
        case Some(message) =>
          val newMessage = message.replace(from, to)
          lastMessage = Some(newMessage)
          location ! SendResponse(credential, newMessage)
        case None =>
          location ! SendResponse(credential, "No messages for you, sorry.")
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

  def getNetwork(): Future[Network] = {
    network match {
      case Some(network) =>
        lastNetworkTime = Some(Calendar.getInstance)
        Future.successful(network)

      case None =>
        val parser = context.actorOf(Props[LogParser])
        val result = parser ? DoParsing(room, nick)
        result map {
          case network: Network =>
            self ! SetNetwork(network)
            parser ! PoisonPill
            network
        }
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
        case Some(network) => network.addPhrase(phrase)
        case None => // will add later on log parse
      }
      true
    } else {
      false
    }
  }

  def generatePhrase(network: Network, length: Integer): String = {
    for (i <- 1 to 25) {
      val phrase = network.generate()
      if (phrase.split(" ").length >= length) {
        return phrase
      }
    }

    "Requested phrase was not found, sorry."
  }
}
