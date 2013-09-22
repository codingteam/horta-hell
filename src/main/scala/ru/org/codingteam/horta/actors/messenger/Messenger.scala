package ru.org.codingteam.horta.actors.messenger

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.jivesoftware.smack.{ConnectionConfiguration, Chat, XMPPConnection}
import org.jivesoftware.smack.filter.{AndFilter, FromContainsFilter, PacketTypeFilter}
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.muc.MultiUserChat
import ru.org.codingteam.horta.actors.database.GetDAORequest
import ru.org.codingteam.horta.actors.pet.PetDAO
import ru.org.codingteam.horta.actors.LogParser
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.UnknownUser
import ru.org.codingteam.horta.Configuration
import scala.concurrent.duration._
import scala.language.postfixOps

class Messenger(val core: ActorRef) extends Actor with ActorLogging {
	import context.dispatcher

	var connection: XMPPConnection = null
	var parser: ActorRef = null
	var privateHandler: ActorRef = null

	override def preStart() {
		parser = context.actorOf(Props[LogParser], "log_parser")
		privateHandler = context.actorOf(Props(new PrivateHandler(self)), "private_handler")

		connection = connect()

		core ! RegisterCommand("say", UnknownUser, self)
		core ! RegisterCommand("♥", UnknownUser, self)
		core ! RegisterCommand("s", UnknownUser, self)
		core ! RegisterCommand("mdiff", UnknownUser, self)
		core ! RegisterCommand("pet", UnknownUser, self)
	}

	override def postStop() {
		disconnect()
	}

	var rooms = Map[String, MultiUserChat]()
	var chats = Map[String, Chat]()

	def receive = {
		case GetDAORequest => {
			sender ! new PetDAO()
		}

		case ExecuteCommand(user, command, arguments) => {
			val location = user.location
			command match {
				case "say" | "♥" => location ! GenerateCommand(user.jid, command, arguments)
				case "s" => location ! ReplaceCommand(user.jid, arguments)
				case "mdiff" => location ! DiffCommand(user.jid, arguments)
				case "pet" => location ! PetCommand(arguments)
			}
		}

		case Reconnect(closedConnection) if connection == closedConnection =>
			disconnect()
            context.children.foreach(context.stop)
			connection = connect()

		case Reconnect(otherConnection) =>
			log.info(s"Ignored reconnect request from connection $otherConnection")

		case JoinRoom(jid) => {
			log.info(s"Joining room $jid")
			val actor = context.system.actorOf(Props(new Room(self, jid)))

			val muc = new MultiUserChat(connection, jid)
			rooms = rooms.updated(jid, muc)

			muc.addMessageListener(new MucMessageListener(jid, actor, log))
			muc.addParticipantListener(new MucParticipantListener(actor))

			val filter = new AndFilter(new PacketTypeFilter(classOf[Message]), new FromContainsFilter(jid))
			connection.addPacketListener(
				new MessageAutoRepeater(self, context.system.scheduler, jid, context.dispatcher),
				filter)

			muc.join(Configuration.nickname)
			muc.sendMessage("Muhahahaha!")
		}

		case ChatOpened(chat) => {
			chats = chats.updated(chat.getParticipant, chat)
			sender ! PositiveReply
		}

		case SendMucMessage(jid, message) => {
			val muc = rooms.get(jid)
			muc match {
				case Some(muc) => muc.sendMessage(message)
				case None =>
			}

			// Sleep to create reasonable pause after sending:
			Thread.sleep((1 second).toMillis)
		}

		case SendChatMessage(jid, message) => {
			val chat = chats.get(jid)
			chat match {
				case Some(chat) => chat.sendMessage(message)
				case None =>
			}

			// Sleep to create reasonable pause after sending:
			Thread.sleep((1 second).toMillis)
		}

		case ProcessCommand(user, message) => {
			core ! ProcessCommand(user, message)
		}
	}

	private def connect(): XMPPConnection = {
		val server = Configuration.server
		log.info(s"Connecting to $server")

		val configuration = new ConnectionConfiguration(server)
		configuration.setReconnectionAllowed(false)

		val connection = new XMPPConnection(configuration)
		val chatManager = connection.getChatManager

		try {
			connection.connect()
		} catch {
			case e: Throwable =>
				log.error(e, "Error while connecting")
				context.system.scheduler.scheduleOnce(10 seconds, self, Reconnect(connection))
				return connection
		}

		connection.addConnectionListener(new XMPPConnectionListener(self, connection))
		chatManager.addChatListener(new ChatListener(self, privateHandler, context.system.dispatcher))

		connection.login(Configuration.login, Configuration.password)
		log.info("Login succeed")

		Configuration.rooms foreach {
			case (roomName, jid) => self ! JoinRoom(jid)
		}

		connection
	}

	private def disconnect() {
		if (connection != null && connection.isConnected) {
			log.info("Disconnecting")
			connection.disconnect()
			log.info("Disconnected")
		}
	}
}
