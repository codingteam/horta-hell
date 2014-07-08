package ru.org.codingteam.horta.plugins.dice

import ru.org.codingteam.horta.plugins.{CommandProcessor, CommandDefinition, BasePlugin}
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.io.Source
import scala.util.Random
import scala.collection.mutable.ListBuffer

private object DiceCommand

/** ***
  * The plugin to roll the dices
  */
class DiceRoller extends BasePlugin with CommandProcessor {

  private val funAnswers = Array(
    "*Потупила глазки* Дядя Форневерик мне говорил не делать этого...",
    "ОРТУ АД!!!",
    "*Чмокнула Миксера*"
  )

  private val errorResponse:String = "Нялайфер мне строго-настрого запретил... не трогать варенье..."

  private def getRandom( min:Int, max:Int ):Int = {
    var random = new Random
    return min + random.nextInt( ( max - min ) + 1 )
  }

  private val min              = 1
  private val maxDiceRollCount = 100 // Maybe it's worth to move it to settings?..

  /**
   * Rolls the dice with certain amount of faces N-times.
   * @param faces amount of the dice faces
   * @param count amount of dice rolls
   * @return all range of dice rolls and its sum.
   */
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

  /**
   * Tries to convert sting to integer
   * @param s
   * @return number if it's correct integer otherwise None
   */
  private def toInt( s:String ): Option[ Int ] = {
    try {
      Some( s.toInt )
    } catch {
      case e:Exception => None
    }
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
          if( arguments.length > 1 ) {
            val someFaces = toInt( arguments( 0 ) )
            val someCcount = toInt( arguments( 1 ) )

            val faces = someFaces.getOrElse( 20 )
            val count = someCcount.getOrElse( 1 )

            if( count > 0 || faces > 1 ) {
              response = diceRoll(faces, count)
            }
            else {
              response = errorResponse
            }
          }
          else {
            response = errorResponse
          }
        }

        Protocol.sendResponse( credential.location, credential, response )
      case _ =>
    }
  }
}

