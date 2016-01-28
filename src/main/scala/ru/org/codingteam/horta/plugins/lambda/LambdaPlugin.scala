package ru.org.codingteam.horta.plugins.lambda

import java.io.InputStreamReader

import me.rexim.morganey.ast.MorganeyBinding
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandProcessor, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import me.rexim.morganey.syntax.LambdaParser
import me.rexim.morganey.{MorganeyInterpreter, ReplHelper}

private object LambdaCommand

class LambdaPlugin extends BasePlugin with CommandProcessor {
  private val initScriptFileName = "morganey/init.morganey"
  private lazy val initScriptReader =
    new InputStreamReader(ClassLoader.getSystemResourceAsStream(initScriptFileName))

  private val globalContext: Seq[MorganeyBinding] =
    MorganeyInterpreter
      .interpretReader(initScriptReader, List())
      .map (_.lastOption.map(_._2).getOrElse(List()))
      .recover {
        case t: Throwable => {
          log.error(t, "Could not load morganey init script")
          List()
        }
      }
      .getOrElse(List())

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

    (token, arguments) match {
      case (LambdaCommand, Array(unparsedTerm, _*)) => {
        val term = LambdaParser.parse(LambdaParser.term, unparsedTerm)
        if (term.successful) {
          val result = term.get.addContext(globalContext).normalOrder()
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
