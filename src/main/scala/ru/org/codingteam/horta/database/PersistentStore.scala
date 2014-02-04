package ru.org.codingteam.horta.actors.database

import akka.actor.{ActorRef, ActorLogging, Actor}
import akka.pattern.ask
import org.h2.jdbcx.JdbcConnectionPool
import java.sql.Connection
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import concurrent.Await
import com.googlecode.flyway.core.Flyway
import javax.sql.DataSource
import ru.org.codingteam.horta.configuration.Configuration

case class RegisterStore(plugin: String, store: DAO)

case object StoreOkReply

case class StoreObject(plugin: String, id: Option[Any], obj: Any)

case class ReadObject(plugin: String, id: Any)

trait DAO {
  def directoryName: String

	def store(connection: Connection, id: Option[Any], obj: Any): Any

	def read(connection: Connection, id: Any): Option[Any]
}

class PersistentStore() extends Actor with ActorLogging {

  val Url = Configuration.storageUrl
  val User = Configuration.storageUser
  val Password = Configuration.storagePassword

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

  var dataSource: DataSource = null
	var daos = Map[String, DAO]()
  var initializedDirectories = Set[String]()

	override def preStart() {
		dataSource = JdbcConnectionPool.create(Url, User, Password)
	}

	override def receive = {
		case RegisterStore(plugin, dao) =>
      daos += plugin -> dao

    case StoreObject(plugin, id, obj) =>
      daos.get(plugin) match {
        case Some(dao) => {
          withConnection { connection =>
              dao.store(connection, id, obj)
              sender ! StoreOkReply
          }
        }

        case None =>
          log.info(s"Cannot store object $obj for plugin $plugin")
      }

    case ReadObject(plugin, id) =>
      daos.get(plugin) match {
        case Some(dao) =>
          val directory = dao.directoryName
          if (!initializedDirectories.contains(directory)) {
            initializeScript(directory)
            initializedDirectories += directory
          }

          withConnection { connection =>
            sender ! dao.read(connection, id)
          }

        case None =>
          log.info(s"Cannot read object $id for plugin $plugin")
      }
  }

  def withConnection[T](action: Connection => T) = {
    val connection = dataSource.getConnection()
    try {
      action(connection)
    } finally {
      connection.close()
    }
  }

  def initializeScript(directory: String) {
    val flyway = new Flyway()

    flyway.setInitOnMigrate(true)
    flyway.setDataSource(dataSource)
    flyway.setLocations(s"classpath:db/$directory")

    flyway.migrate()
  }
}
