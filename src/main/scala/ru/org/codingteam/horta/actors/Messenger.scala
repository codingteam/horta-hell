package ru.org.codingteam.horta.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.{Dollar, Slash}
import org.jivesoftware.smack.{PacketListener, XMPPConnection}
import org.jivesoftware.smack.filter.{AndFilter, FromContainsFilter, PacketFilter, PacketTypeFilter}
import org.jivesoftware.smack.packet.{Packet, Presence, Message}
import org.jivesoftware.smackx.muc.MultiUserChat
import ru.org.codingteam.horta.Configuration
import ru.org.codingteam.horta.messages._
import ru.org.codingteam.horta.security.UnknownUser
import scala.concurrent.duration._

class Messenger(val core: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher

  var connection: XMPPConnection = null
  var parser: ActorRef = null

  override def preStart() = {
    val server = Configuration.server
    log.info(s"Connecting to $server")

    connection = new XMPPConnection(server)
    connection.connect()
    connection.login(Configuration.login, Configuration.password)
    log.info("Login succeed")

    Configuration.rooms foreach { case (roomName, jid) => self ! JoinRoom(jid) }
    core ! RegisterCommand(Dollar, "say", UnknownUser, self)
    core ! RegisterCommand(Dollar, "♥", UnknownUser, self)
    core ! RegisterCommand(Slash, "s", UnknownUser, self)
    core ! RegisterCommand(Dollar, "mdiff", UnknownUser, self)
    core ! RegisterCommand(Dollar, "pet", UnknownUser, self)

    parser = context.actorOf(Props[LogParser])
  }

  var rooms = Map[String, MultiUserChat]()

  def receive = {
    case ExecuteCommand(user, command, arguments) => {
      val location = user.location
      command match {
        case "say" | "♥" => location ! GenerateCommand(user.jid, command)
        case "s"         => location ! ReplaceCommand(user.jid, arguments)
        case "mdiff"     => location ! DiffCommand(user.jid, arguments)
        case "pet"       => location ! PetCommand(arguments)
      }
    }

    case JoinRoom(jid) => {
      log.info(s"Joining room $jid")
      val actor = context.system.actorOf(Props(new Room(self, parser, jid)), jid)
      val muc = new MultiUserChat(connection, jid)
      rooms = rooms.updated(jid, muc)

      muc.addMessageListener(new PacketListener {
        def processPacket(packet: Packet) {
          log.info(s"Packet received from $jid")
          packet match {
            case message: Message => {
              // Little trick to ignore historical messages:
              val extension = message.getExtension("delay", "urn:xmpp:delay")
              if (extension == null) {
                actor ! UserMessage(message)
              }
            }
          }
        }
      })

      muc.addParticipantListener(new PacketListener {
        def processPacket(packet: Packet) {
          packet match {
            case presence: Presence => {
              actor ! UserPresence(presence)
            }
          }
        }
      })

      val filter = new AndFilter(new PacketTypeFilter(classOf[Message]), new FromContainsFilter(jid))
      connection.addPacketListener(
        new PacketListener {
          def processPacket(packet: Packet) {
            packet match {
              case message: Message => {
                val error = packet.getError
                if (error != null && error.getCondition == "resource-constraint") {
                  val body = message.getBody
                  context.system.scheduler.scheduleOnce(1 second) {
                    self ! SendMessage(jid, "[Re]" + body)
                  }

                  // Sleep to create reasonable pause:
                  Thread.sleep((1 second).toMillis)
                }
              }
            }
          }
        },
        filter)

      muc.join(Configuration.nickname)
      muc.sendMessage("Muhahahaha!")
    }

    case SendMessage(room, message) => {
      val muc = rooms.get(room)
      muc match {
        case Some(muc) => muc.sendMessage(message)
        case None =>
      }
    }

    case ProcessCommand(user, message) => {
      core ! ProcessCommand(user, message)
    }
  }
}
