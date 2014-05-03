package ru.org.codingteam.horta.plugins.markov

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import ru.org.codingteam.horta.messages._
import scala.language.postfixOps
import ru.org.codingteam.horta.plugins.{PluginDefinition, CommandDefinition, CommandPlugin}
import ru.org.codingteam.horta.security.{CommonAccess, Credential}
import ru.org.codingteam.horta.configuration.{RoomDescriptor, Configuration}

case object SayCommand

case object ReplaceCommand

class MarkovPlugin() extends CommandPlugin {

  // TODO: Drop inactive users?
  var users = Map[String, ActorRef]()

  override def pluginDefinition = PluginDefinition(
    "markov",
    true,
    List(CommandDefinition(CommonAccess, "say", SayCommand), CommandDefinition(CommonAccess, "s", ReplaceCommand)),
    None)

  override def processMessage(credential: Credential, message: String) {
    val user = getUser(credential)
    user ! UserPhrase(message)
  }

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case SayCommand =>
        generatePhrase(credential, arguments)
      case ReplaceCommand =>
        replace(credential, arguments)
    }
  }

  def isMyself(credential: Credential): Boolean = {
    (Configuration.roomDescriptors find {rd => rd.room == credential.roomName.getOrElse("")} map {rd => rd.nickname} getOrElse(Configuration.dftName)) == credential.name
  }

  def generatePhrase(credential: Credential, arguments: Array[String]) {
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

        if (! isMyself(credential)) {
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
            user ! GeneratePhrase(credential, length, false)
          }
        }

      case _ =>
    }
  }

  def replace(credential: Credential, arguments: Array[String]) {
    val location = credential.location
    val nick = credential.name

    if (! isMyself(credential)) {
      arguments match {
        case Array(from, to, _*) if from != "" => {
          val user = getUser(credential)
          user ! ReplaceRequest(credential, from, to)
        }

        case _ =>
          location ! SendResponse(credential, "Wrong arguments.")
      }
    }
  }

  def getUser(credential: Credential) = {
    val roomName = credential.roomName.getOrElse("")
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
