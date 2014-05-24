package ru.org.codingteam.horta.database

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import com.googlecode.flyway.core.Flyway
import java.sql.Connection
import javax.sql.DataSource
import org.h2.jdbcx.JdbcConnectionPool
import ru.org.codingteam.horta.configuration.Configuration
import scala.concurrent.duration._
import scala.language.postfixOps

case class StoreObject(plugin: String, id: Option[Any], obj: Any)

case class ReadObject(plugin: String, id: Any)

case class DeleteObject(plugin: String, id: Any)

trait DAO {
  def directoryName: String

  /**
   * Store an object in the database.
   * @param connection connection to access the database.
   * @param id object id (if null then it should be generated).
   * @param obj stored object.
   * @return stored object id (or None if object was not stored).
   */
  def store(connection: Connection, id: Option[Any], obj: Any): Option[Any]

  /**
   * Read an object from the database.
   * @param connection connection to access the database.
   * @param id object id.
   * @return stored object or None if object not found.
   */
  def read(connection: Connection, id: Any): Option[Any]

  /**
   * Delete an object from the database.
   * @param connection connection to access the database.
   * @param id object id.
   * @return true if object was successfully deleted, false otherwise.
   */
  def delete(connection: Connection, id: Any): Boolean
}

class PersistentStore(storages: Map[String, DAO]) extends Actor with ActorLogging {

  val Url = Configuration.storageUrl
  val User = Configuration.storageUser
  val Password = Configuration.storagePassword

  implicit val timeout = Timeout(60 seconds)

  var dataSource: DataSource = null
  var initializedDirectories = Set[String]()

  override def preStart() {
    dataSource = JdbcConnectionPool.create(Url, User, Password)
  }

  override def receive = {
    case StoreObject(plugin, id, obj) =>
      storages.get(plugin) match {
        case Some(dao) =>
          initializeDatabase(dao)
          withConnection { connection =>
            sender ! dao.store(connection, id, obj)
          }

        case None =>
          log.info(s"Cannot store object $obj for plugin $plugin")
      }

    case ReadObject(plugin, id) =>
      storages.get(plugin) match {
        case Some(dao) =>
          initializeDatabase(dao)
          withConnection { connection =>
            sender ! dao.read(connection, id)
          }

        case None =>
          log.info(s"Cannot read object $id for plugin $plugin")
      }

    case DeleteObject(plugin, id) =>
      storages.get(plugin) match {
        case Some(dao) =>
          initializeDatabase(dao)
          withConnection { connection =>
            sender ! dao.delete(connection, id)
          }

        case None =>
          log.info(s"Cannot delete object $id for plugin $plugin")
      }
  }

  private def initializeDatabase(dao: DAO) {
    val directory = dao.directoryName
    if (!initializedDirectories.contains(directory)) {
      initializeScript(directory)
      initializedDirectories += directory
    }
  }

  private def withConnection[T](action: Connection => T) = {
    val connection = dataSource.getConnection()
    try {
      action(connection)
    } finally {
      connection.close()
    }
  }

  private def initializeScript(directory: String) {
    val flyway = new Flyway()

    flyway.setInitOnMigrate(true)
    flyway.setDataSource(dataSource)
    flyway.setLocations(s"db/$directory")

    // Do our best to fix any errors:
    flyway.repair()
    flyway.migrate()
  }
}
