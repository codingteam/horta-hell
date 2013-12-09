package ru.org.codingteam.horta.plugins.markov

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import ru.org.codingteam.horta.messages._
import scala.language.postfixOps
import ru.org.codingteam.horta.Configuration
import ru.org.codingteam.horta.plugins.{CommandDefinition, CommandPlugin}
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

class MarkovPlugin() extends CommandPlugin {
  // TODO: Drop inactive users?
  private case object SayCommand
  private case object FavoriteCommand
  private case object ReplaceCommand
  private case object DiffCommand

  var users = Map[String, ActorRef]()

  override def commandDefinitions = List(
    CommandDefinition(CommonAccess, "say", SayCommand),
    CommandDefinition(CommonAccess, "♥", FavoriteCommand),
    CommandDefinition(CommonAccess, "s", ReplaceCommand),
    CommandDefinition(CommonAccess, "mdiff", DiffCommand)
  )

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case SayCommand =>
        generatePhrase(false, credential, arguments)
      case FavoriteCommand =>
        generatePhrase(true, credential, arguments)
      case ReplaceCommand =>
        replace(credential, arguments)
      case DiffCommand =>
        diff(credential, arguments)
    }
  }

  def generatePhrase(favorite: Boolean, credential: Credential, arguments: Array[String]) {
    arguments match {
      case Array(_) | Array() =>
        val length = arguments match {
          case Array(lengthParameter, _*) =>
            try {
              lengthParameter.toInt
            } catch {
              case _: NumberFormatException => 1
            }
          case _ => 1
        }

        val nick = credential.name
        if (nick != Configuration.nickname) {
          val user = getUser(credential)
          val location = credential.location
            if (Math.random() > 0.99) {
              location ! SendResponse(credential, "пффффш")
              location ! SendResponse(credential, "шпфффф")
              location ! SendResponse(credential, "я твой Хортец!")
            } else if (Math.random() < 0.01) {
              location ! SendResponse(credential, "BLOOD GORE DESTROY")
              for (i <- 1 to 10) {
                user ! GeneratePhrase(credential, 1, true)
              }
            } else {
              user ! GeneratePhrase(if (favorite) "ForNeVeR" else nick, length, false)
            }
        }

      case _ =>
    }
  }

  def replace(credential: Credential, arguments: Array[String]) {
    val location = credential.location
    val nick = credential.name

    if (nick != Configuration.nickname) {
      arguments match {
        case Array(from, to, _*) if from != "" => {
          val user = getUser(credential)
          for {
            responseFromUser <- user ? ReplaceRequest(arguments(0), arguments(1))
          } yield responseFromUser match {
            case ReplaceResponse(message) =>
              location ! SendResponse(credential, message)
          }
        }

        case _ =>
          location ! SendResponse(credential, "Wrong arguments.")
      }
    }
  }

  def diff(credential: Credential, arguments: Array[String]) {
    val location = credential.location
    val nick = credential.name

    if (nick != Configuration.nickname) {
      if (arguments.length < 2) {
        location ! SendResponse(credential, "Wrong arguments.")
      } else {
        val nick1 = arguments(0)
        val nick2 = arguments(1)
        val user1 = users.get(nick1)
        val user2 = users.get(nick2)

        if (user1.isDefined && user2.isDefined) {
          user1.get ! CalculateDiff(nick, nick1, nick2, user2.get)
        } else {
          location ! SendResponse(credential, "User not found.")
        }
      }
    }
  }

	def getUser(credential: Credential) = {
    val roomName = credential.roomName.get
    val name = credential.name

    val user = users.get(credential.name)
		user match {
			case Some(u) => u
			case None => {
				val user = context.actorOf(Props(new MarkovUser(roomName, name)))
				users = users.updated(name, user)
				user
			}
		}
	}
}
