package ru.org.codingteam.horta.plugins.lambda

import java.io.InputStreamReader

import me.rexim.morganey.MorganeyInterpreter._
import me.rexim.morganey.ReplHelper
import me.rexim.morganey.ast.{LambdaTerm, MorganeyBinding}
import me.rexim.morganey.reduction.Computation
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
  private val computationTimeout = 2 seconds
  private val outputThreshold = 30

  private var currentComputation: Option[Computation[LambdaTerm]] = None

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
      case (LambdaCommand, Array(rawTerm, _*)) => {
        currentComputation match {
          case Some(computation) if !computation.future.isCompleted =>
            respond("The previous computation is not cancelled yet. Please wait or contact Dr. ForNeVeR.")
          case _ => evalTerm(rawTerm)
        }
      }

      case (LambdaBindingsCommand, _) => {
        respond(globalContext.take(outputThreshold).map(_.variable.name).mkString(", "))
      }

      case _ => // ignore
    }
  }

  private def evalTerm(rawTerm: String)(implicit credential: Credential) = {
    val term = LambdaParser.parse(LambdaParser.term, rawTerm)
    if (term.successful) {
      val computation = term.get.addContext(globalContext).norReduceComputation()
      currentComputation = Some(computation)
      try {
        val term = Await.result(computation.future, computationTimeout)
        respond(ReplHelper.smartPrintTerm(term))
      } catch {
        case e: TimeoutException =>
          computation.cancel
          respond("The computation took too long and was scheduled for the cancellation")
      }
    } else {
      respond(term.toString)
    }
  }

  private def respond(responseMessage: String)(implicit credential: Credential) = {
    Protocol.sendResponse(credential.location, credential, responseMessage)
  }
}
