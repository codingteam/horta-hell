package ru.org.codingteam.horta.plugins.bash

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.io.Source
import ru.org.codingteam.horta.messages.SendResponse
import ru.org.codingteam.horta.plugins.{PluginDefinition, CommandDefinition, CommandPlugin}
import org.joda.time.{DateTime, Period}

private object BashCommand

class BashPlugin extends CommandPlugin {

  private val bashImForWebUrl = "http://bash.im/forweb/?u"

  private val coolDownPeriod = 5

  private var lastRequestDateTime: DateTime = DateTime.now()

  def pluginDefinition = PluginDefinition(
    "bash",
    false,
    List(CommandDefinition(CommonAccess, "bash", BashCommand)),
    None)

  override def processCommand(
                               credential: Credential,
                               token: Any,
                               arguments: Array[String]) = {
    token match {
      case BashCommand =>
        val bashImForWebResponse = Source.fromURL(bashImForWebUrl)(scala.io.Codec.UTF8).mkString

        BashForWebResponseParser(bashImForWebResponse) match {
          case Some(BashQuote(number, rate, text)) =>
            val now = DateTime.now()
            val period = new Period(lastRequestDateTime, now)
            var response = ""

            if (period.getSeconds > coolDownPeriod) { // TODO: Move to the core facility
              response = s"$number $rate\n$text"
              lastRequestDateTime = now
            }
            else {
              response = "Не так часто, пожалуйста."
            }

            credential.location ! SendResponse(credential, response)

          case None =>
        }

      case _ =>
    }
  }

}
