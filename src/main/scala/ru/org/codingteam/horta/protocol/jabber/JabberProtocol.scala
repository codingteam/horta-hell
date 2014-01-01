package ru.org.codingteam.horta.protocol.jabber

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import org.jivesoftware.smack.{Chat, ConnectionConfiguration, XMPPConnection}
import org.jivesoftware.smack.filter.{AndFilter, FromContainsFilter, PacketTypeFilter}
import org.jivesoftware.smack.packet.{Presence, Message}
import org.jivesoftware.smackx.muc.MultiUserChat
import ru.org.codingteam.horta.messages._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.language.postfixOps
import ru.org.codingteam.horta.messages.SendMucMessage
import ru.org.codingteam.horta.messages.SendChatMessage
import scala.Some
import ru.org.codingteam.horta.messages.UserPresence
import ru.org.codingteam.horta.messages.ChatOpened
import ru.org.codingteam.horta.messages.Reconnect
import ru.org.codingteam.horta.messages.JoinRoom
import ru.org.codingteam.horta.configuration.Configuration

class JabberProtocol() extends Actor with ActorLogging {
	case class RoomDefinition(chat: MultiUserChat, actor: ActorRef)

	import context.dispatcher
	implicit val timeout = Timeout(1 minute)

  val core = context.actorSelection("/user/core")

	var connection: XMPPConnection = null
  var chats = Map[String, Chat]()

	var privateHandler: ActorRef = null
  var rooms = Map[String, RoomDefinition]()

	override def preStart() {
		privateHandler = context.actorOf(Props(new PrivateMessageHandler(self)), "privateHandler")
    initializeConnection()
	}

  override def postRestart(reason: Throwable) {
    initializeConnection()
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    disconnect()
  }

  override def postStop() {
		disconnect()
	}

	def receive = {
		case Reconnect(closedConnection) if connection == closedConnection =>
			disconnect()
      context.children.foreach(context.stop)
			initializeConnection()

		case Reconnect(otherConnection) =>
			log.info(s"Ignored reconnect request from connection $otherConnection")

		case JoinRoom(jid) => {
      log.info(s"Joining room $jid")
			val actor = context.system.actorOf(Props(new MucMessageHandler(self, jid)), jid)

			val muc = new MultiUserChat(connection, jid)
			rooms = rooms.updated(jid, RoomDefinition(muc, actor))

			muc.addMessageListener(new MucMessageListener(jid, actor, log))
			muc.addParticipantListener(new MucParticipantListener(actor))

			val filter = new AndFilter(new PacketTypeFilter(classOf[Message]), new FromContainsFilter(jid))
			connection.addPacketListener(
				new MessageAutoRepeater(self, context.system.scheduler, jid, context.dispatcher),
				filter)

			muc.join(Configuration.nickname)
			muc.getOccupants.foreach { occupant =>
				actor ! UserPresence(occupant, Presence.Type.available)
			}

			muc.sendMessage("Hell installed.")
		}

		case ChatOpened(chat) => {
			chats = chats.updated(chat.getParticipant, chat)
			sender ! PositiveReply
		}

		case SendMucMessage(jid, message) => {
			val muc = rooms.get(jid)
			muc match {
				case Some(muc) => muc.chat.sendMessage(message)
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
	}

  private def initializeConnection() {
    connection = connect()
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
