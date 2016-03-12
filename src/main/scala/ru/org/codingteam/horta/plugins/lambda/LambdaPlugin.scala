package ru.org.codingteam.horta.plugins.lambda

import java.io.InputStreamReader

import me.rexim.morganey.MorganeyInterpreter._
import me.rexim.morganey.ReplHelper
import me.rexim.morganey.ast.MorganeyBinding
import me.rexim.morganey.syntax.LambdaParser
import me.rexim.morganey.reduction.NormalOrder._
import ru.org.codingteam.horta.core.TryWith
import ru.org.codingteam.horta.plugins.{BasePlugin, CommandDefinition, CommandProcessor}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

private object LambdaCommand
private object LambdaBindingsCommand

class LambdaPlugin extends BasePlugin with CommandProcessor {
  private val initScriptResourceName = "/morganey/init.morganey"

  private lazy val globalContext: Seq[MorganeyBinding] =
    TryWith(new InputStreamReader(getClass.getResourceAsStream(initScriptResourceName))) { reader =>
      readNodes(reader)
    } map { nodes =>
      evalNodes(nodes)(List()).context
    } recover {
      case t: Throwable =>
        log.error(t, s"Cannot read the Global Morganey Context from $initScriptResourceName")
        List()
    } getOrElse List()

  /**
    * Plugin name.
    * @return unique plugin name.
    */
  override protected def name: String = "lambda"

  override protected def commands = List(
    CommandDefinition(CommonAccess, "lambda", LambdaCommand),
    CommandDefinition(CommonAccess, "lambda-bindings", LambdaBindingsCommand)
  )

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
          val computation = term.get.addContext(globalContext).norReduceComputation()
          try {
            val term = Await.result(computation.future, 2 seconds)
            respond(ReplHelper.smartPrintTerm(term))
          } catch {
            case e: TimeoutException =>
              computation.cancel
              respond("Computation took too long and was cancelled by a timeout")
          }
        } else {
          respond(term.toString)
        }
      }

      case (LambdaBindingsCommand, _) => {
        val outputThreshold = 30
        respond(globalContext.take(outputThreshold).map(_.variable.name).mkString(", "))
      }

      case _ => // ignore
    }
  }

  private def respond(responseMessage: String)(implicit credential: Credential) = {
    Protocol.sendResponse(credential.location, credential, responseMessage)
  }
}
