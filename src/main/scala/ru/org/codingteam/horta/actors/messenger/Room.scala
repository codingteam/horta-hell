package ru.org.codingteam.horta.actors.messenger

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.jivesoftware.smack.packet.Presence
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.{RoomVisitor, User}
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.actors.pet.Pet
import ru.org.codingteam.horta.Configuration

class Room(val messenger: ActorRef, val room: String) extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	var users = Map[String, ActorRef]()
	var pet: ActorRef = null
	var lastMessage: Option[String] = None

	override def preStart() {
		pet = context.actorOf(Props(new Pet(self, room)))
	}

	def receive = {
		case GetJID() =>
			sender ! room

		case UserMessage(message) => {
			val jid = message.getFrom
			val text = message.getBody

			val nick = nickByJid(jid)
			val user = userByNick(nick)

			user ! UserPhrase(text)
			messenger ! ProcessCommand(getUserObject(jid), text)
		}

		case UserPresence(presence) => {
			val jid = presence.getFrom
			val nick = nickByJid(jid)
			val presenceType = presence.getType
			if (nick == "zxc" && presenceType == Presence.Type.available) {
				sendMessage(if (Math.random() > 0.5) ".z" else "zxc: осечка!")
			}
		}

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

		case PetCommand(command) => {
			pet ! PetCommand(command)
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

	def sendMessage(message: String) {
		messenger ! SendMucMessage(room, message)
	}

	def prepareResponse(nick: String, message: String) = s"$nick: $message"

	def nickByJid(jid: String) = {
		val args = jid.split('/')
		if (args.length > 1) {
			args(1)
		} else {
			args(0)
		}
	}

	def getUserObject(jid: String) = {
		val realJid = None // TODO: Use admin access to know the real JID if possible.
		val privileges = RoomVisitor // TODO: Get real room privileges.
		User(realJid, Some(room), Some(nickByJid(jid)), Some(privileges))
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
