package ru.org.codingteam.horta.protocol.xmpp

import java.util.concurrent.Executors

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import org.jivesoftware.smack._
import ru.org.codingteam.horta.configuration.Configuration
import ru.org.codingteam.horta.protocol.{GlobalUserId, IProtocol, Participant, RoomId}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

// TODO: Convert this into a state machine. ~ F
class XmppProtocol(logger: LoggingAdapter, core: ActorRef) extends IProtocol {

  private val reconnectionPause = 10.seconds

  private val executor = Executors.newSingleThreadExecutor()
  implicit val context = ExecutionContext.fromExecutor(executor)
  private var connection: Option[XMPPConnection] = None
  private var stopping = false

  override def dispose(): Unit = {
    Future({
      connection.foreach(c => if (c.isConnected) c.disconnect())
      stopping = true
    }).andThen({ case _ => executor.shutdownNow() })
      .wait()
  }

  override def joinRoom(roomId: RoomId): Unit = ???

  override def getParticipants(roomId: RoomId): Future[Map[String, Participant]] = ???

  override def sendRoomMessage(roomId: RoomId, message: String): Future[Unit] = ???

  override def sendPrivateMessage(userId: GlobalUserId, message: String): Future[Unit] = ???

  private def withConnection[T](title: String, action: XMPPConnection => T): Future[T] = {
    Future({
      if (stopping) {
        throw new Exception(s"XmppProtocol $this is shutting down")
      }

      try {
        action(connection.getOrElse(connect()))
      } catch {
        case t: Throwable =>
          logger.error(t, s"Action '$title' executing error")
          throw t
      }
    })
  }

  // TODO: Cannot place @tailrec here ~ F
  // @tailrec
  private def connect(): XMPPConnection = {
    val server = Configuration.server
    logger.info(s"Connecting to $server")

    val configuration = new ConnectionConfiguration(server)
    configuration.setReconnectionAllowed(false)

    val connection = new XMPPConnection(configuration)
    val chatManager = connection.getChatManager

    try {
      connection.connect()
    } catch {
      case e: Throwable =>
        logger.error(e, "Error while connecting")
        Thread.sleep(reconnectionPause.toMillis)
        // TODO: This is not a good thing. Consider the connection that cannot be connected at all. It will try and
        // try and try, and won't be able to reconnect or even stop. Maybe consider a state machine or an actor here.
        // ~ F
        return connect()
    }

    connection.addConnectionListener(connectionListener())
    chatManager.addChatListener(chatListener())

    connection.login(Configuration.login, Configuration.password) // TODO: Catch login errors. ~ F
    logger.info("Login succeed")

    connection
  }

  private def reconnect(): Unit = {
    Future({
      connection.foreach(c => if (c.isConnected) c.disconnect())
      connection = Some(connect())
    })
  }

  private def connectionListener(): ConnectionListener = new ConnectionListener() {
    override def reconnectionFailed(e: Exception): Unit = ()
    override def reconnectionSuccessful(): Unit = ()
    override def connectionClosedOnError(e: Exception): Unit = reconnect()
    override def connectionClosed(): Unit = ()
    override def reconnectingIn(seconds: Int): Unit = ()
  }

  private def chatListener(): ChatManagerListener = new ChatManagerListener {
    override def chatCreated(chat: Chat, createdLocally: Boolean): Unit = ??? // TODO: Subscribe to chat messages ~ F
  }
}
