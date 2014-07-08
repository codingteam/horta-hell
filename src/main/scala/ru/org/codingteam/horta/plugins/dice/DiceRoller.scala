package ru.org.codingteam.horta.plugins.dice

import ru.org.codingteam.horta.plugins.{CommandProcessor, CommandDefinition, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.io.Source
import scala.util.Random
import scala.collection.mutable.ListBuffer

private object DiceCommand

class DiceRoller extends BasePlugin with CommandProcessor {

  private val funAnswers = Array(
    "*Потупила глазки* Дядя Форневерик мне говорил не делать этого...",
    "ОРТУ АД!!!",
    "*Чмокнула Миксера*"
  )

  private def getRandom( min:Int, max:Int ):Int = {
    var random = new Random
    return min + random.nextInt( ( max - min ) + 1 )
  }

  private val min              = 1
  private val maxDiceRollCount = 100 // Maybe it's worth to move it to settings?..

  private def diceRoll( faces:Int, count:Int ):String = {
    var data = new ListBuffer[Int]()

    val upLimit = if( count > maxDiceRollCount ) maxDiceRollCount else count

    for( i <- 1 to upLimit ) {
      data += getRandom( min, faces )
    }

    var result = ""
    var sum    = 0

    data.map( (arg:Int) => {
        result += " " + arg.toString
        sum    += arg
      }
    )

    result += " | " + sum.toString()
    return result
  }

  override def name = "dice"

  override def commands = List( CommandDefinition( CommonAccess, "dice", DiceCommand ) )

  override def processCommand( credential: Credential,  token: Any, arguments: Array[ String ] ) = {
    token match {
      case DiceCommand =>
        var response = ""

        if( Math.random() > 0.90 ) {
          response = funAnswers( getRandom( 0, 2 ) )
        }
        else {
          val faces = arguments( 0 ).toInt
          val count = arguments( 1 ).toInt
          response = diceRoll  ( faces, count )
        }

        Protocol.sendResponse( credential.location, credential, response )
      case _ =>
    }
  }
}