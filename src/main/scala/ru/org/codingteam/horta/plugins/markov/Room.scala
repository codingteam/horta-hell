package ru.org.codingteam.horta.plugins.markov

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.org.codingteam.horta.messages._
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.Configuration

class Room(val messenger: ActorRef, val room: String) extends Actor with ActorLogging {
  // TODO: Factor room functionality to 2 classes: one for markov interactions and another for the room itself.
  import context.dispatcher
  implicit val timeout = Timeout(60 seconds)

	var users = Map[String, ActorRef]()
	var lastMessage: Option[String] = None

	def receive = {
		case GenerateCommand(jid, command, arguments) => {
			arguments match {
				case Array(_) | Array() =>
					val length = arguments match {
						case Array(length, _*) =>
							try {
								length.toInt
							} catch {
								case _: NumberFormatException => 1
							}
						case _ => 1
					}

					val nick = nickByJid(jid)
					if (nick != Configuration.nickname) {
						val user = userByNick(nick)
						if (command == "say" || command == "♥") {
							if (Math.random() > 0.99) {
								messenger ! SendMucMessage(room, "пффффш")
								messenger ! SendMucMessage(room, "шпфффф")
								messenger ! SendMucMessage(room, prepareResponse(nick, "я твой Хортец!"))
							} else if (Math.random() < 0.01) {
								messenger ! SendMucMessage(room, prepareResponse(nick, "BLOOD GORE DESTROY"))
								for (i <- 1 to 10) {
									user ! GeneratePhrase(nick, 1, true)
								}
							} else {
								user ! GeneratePhrase(if (command != "♥") nick else "ForNeVeR", length, false)
							}
						}
					}

				case _ =>
			}
		}

		case ReplaceCommand(jid, arguments) => {
			val nick = nickByJid(jid)
			if (nick != Configuration.nickname) {
				arguments match {
					case Array(from, to, _*) if from != "" => {
						val user = userByNick(nick)
						for {
							responseFromUser <- user ? ReplaceRequest(arguments(0), arguments(1))
						} yield responseFromUser match {
							case ReplaceResponse(message) => messenger ! SendMucMessage(room, prepareResponse(nick, message))
						}
					}

					case _ => {
						sendMessage(prepareResponse(nick, "Wrong arguments."))
					}
				}
			}
		}

		case DiffCommand(jid, arguments) => {
			val nick = nickByJid(jid)
			if (nick != Configuration.nickname) {
				if (arguments.length < 2) {
					sendMessage(prepareResponse(nick, "Wrong arguments."))
				} else {
					val nick1 = arguments(0)
					val nick2 = arguments(1)
					val user1 = users.get(nick1)
					val user2 = users.get(nick2)

					if (user1.isDefined && user2.isDefined) {
						user1.get ! CalculateDiff(nick, nick1, nick2, user2.get)
					} else {
						sendMessage(prepareResponse(nick, "User not found."))
					}
				}
			}
		}

		case ParsedPhrase(nick, message) => {
			val user = userByNick(nick)
			user ! AddPhrase(message)
		}

		case GeneratedPhrase(forNick, phrase) => {
			sendMessage(prepareResponse(forNick, phrase))
		}

		case CalculateDiffResponse(forNick, nick1, nick2, diff) => {
			sendMessage(prepareResponse(forNick, s"Difference between $nick1 and $nick2 is $diff."))
		}

		case PetResponse(message) => {
			sendMessage(message)
		}
	}

	def userByNick(nick: String) = {
		val user = users.get(nick)
		user match {
			case Some(u) => u
			case None => {
				val user = context.actorOf(Props(new RoomUser(room, nick)))
				users = users.updated(nick, user)
				user
			}
		}
	}
}
