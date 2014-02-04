package ru.org.codingteam.horta.plugins.bash

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.io.Source
import ru.org.codingteam.horta.messages.SendResponse
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandPlugin}
import org.joda.time.{DateTime, Period}

class BashPlugin extends CommandPlugin {

  private object BashCommand

  private val bashImForWebUrl = "http://bash.im/forweb/?u"

  private val coolDownPeriod = 3

  private var lastRequestDateTime: DateTime = DateTime.now()

  def commandDefinitions: List[CommandDefinition] =
    List(CommandDefinition(CommonAccess, "bash", BashCommand))

  override def processCommand(
                               credential: Credential,
                               token: Any,
                               arguments: Array[String]) = {
    token match {
      case BashCommand => {
        val bashImFroWebResponse = Source.fromURL(bashImForWebUrl)(scala.io.Codec.UTF8).mkString

        BashForWebResponseParser(bashImFroWebResponse) match {
          case Some(BashQuote(number, text)) => {
            val now = DateTime.now()
            val period = new Period(lastRequestDateTime, now)
            var response = ""

            if (period.getSeconds > coolDownPeriod) {
              response = s"$number\n$text"
              lastRequestDateTime = now
            }
            else {
              response = "Не так часто, пожалуйста."
            }

            credential.location ! SendResponse(credential, response)
          }

          case None =>
        }
      }

      case _ =>
    }
  }

}