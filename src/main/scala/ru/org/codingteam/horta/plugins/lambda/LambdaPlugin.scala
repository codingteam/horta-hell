package ru.org.codingteam.horta.plugins.lambda

import me.rexim.morganey.ast.MorganeyBinding
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import me.rexim.morganey.syntax.LambdaParser
import me.rexim.morganey.ast.LambdaTermHelpers._
import me.rexim.morganey.ReplHelper

private object LambdaCommand

class LambdaPlugin extends BasePlugin with CommandProcessor {
  /**
    * Plugin name.
    * @return unique plugin name.
    */
  override protected def name: String = "lambda"

  override protected def commands = List(CommandDefinition(CommonAccess, "lambda", LambdaCommand))

  /**
    * Process a command.
    * @param credential a credential of a user executing the command.
    * @param token token registered for command.
    * @param arguments command argument array.
    */
  override protected def processCommand(credential: Credential,
                                        token: Any,
                                        arguments: Array[String]): Unit = {
    implicit val c = credential
    implicit val l = log

    val context = Seq[MorganeyBinding](MorganeyBinding(lvar("I"), lfunc("x", lvar("x"))))

    (token, arguments) match {
      case (LambdaCommand, Array(unparsedTerm, _*)) => {
        val term = LambdaParser.parse(LambdaParser.term, unparsedTerm)
        if (term.successful) {
          val result = term.get.addContext(context).normalOrder()
          respond(ReplHelper.smartPrintTerm(result))
        } else {
          respond(term.toString)
        }
      }
      case _ => // ignore
    }
  }

  private def respond(responseMessage: String)(implicit credential: Credential) = {
    Protocol.sendResponse(credential.location, credential, responseMessage)
  }
}
