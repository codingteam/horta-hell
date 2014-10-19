package ru.org.codingteam.horta.database

import javax.sql.DataSource

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import com.googlecode.flyway.core.Flyway
import org.h2.jdbcx.JdbcConnectionPool
import ru.org.codingteam.horta.configuration.Configuration
import scalikejdbc.{ConnectionPool, DB, DBSession, DataSourceConnectionPool}

import scala.concurrent.duration._
import scala.language.postfixOps

case class StoreObject(plugin: String, id: Option[Any], obj: Any)

case class ReadObject(plugin: String, id: Any)

case class DeleteObject(plugin: String, id: Any)

trait DAO {

  /**
   * Schema name for current DAO.
   * @return schema name.
   */
  def schema: String

  /**
   * Store an object in the database.
   * @param session session to access the database.
   * @param id object id (if null then it should be generated).
   * @param obj stored object.
   * @return stored object id (or None if object was not stored).
   */
  def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any]

  /**
   * Read an object from the database.
   * @param session session to access the database.
   * @param id object id.
   * @return stored object or None if object not found.
   */
  def read(implicit session: DBSession, id: Any): Option[Any]

  /**
   * Delete an object from the database.
   * @param session session to access the database.
   * @param id object id.
   * @return true if object was successfully deleted, false otherwise.
   */
  def delete(implicit session: DBSession, id: Any): Boolean

}

class PersistentStore(storages: Map[String, DAO]) extends Actor with ActorLogging {

  val Url = Configuration.storageUrl
  val User = Configuration.storageUser
  val Password = Configuration.storagePassword

  implicit val timeout = Timeout(60 seconds)

  var dataSource: DataSource = null
  var initializedSchemas = Set[String]()

  override def preStart() {
    dataSource = JdbcConnectionPool.create(Url, User, Password)
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  }

  override def receive = {
    case StoreObject(plugin, id, obj) =>
      storages.get(plugin) match {
        case Some(dao) =>
          initializeDatabase(dao)
          withTransaction { session =>
            sender ! dao.store(session, id, obj)
          }

        case None =>
          log.info(s"Cannot store object $obj for plugin $plugin")
      }

    case ReadObject(plugin, id) =>
      storages.get(plugin) match {
        case Some(dao) =>
          initializeDatabase(dao)
          withTransaction { session =>
            sender ! dao.read(session, id)
          }

        case None =>
          log.info(s"Cannot read object $id for plugin $plugin")
      }

    case DeleteObject(plugin, id) =>
      storages.get(plugin) match {
        case Some(dao) =>
          initializeDatabase(dao)
          withTransaction { session =>
            sender ! dao.delete(session, id)
          }

        case None =>
          log.info(s"Cannot delete object $id for plugin $plugin")
      }
  }

  private def initializeDatabase(dao: DAO) {
    val schema = dao.schema
    if (!initializedSchemas.contains(schema)) {
      initializeScript(schema)
      initializedSchemas += schema
    }
  }

  private def withTransaction[T](action: (DBSession) => T) = {
    DB localTx { session =>
      action(session)
    }
  }

  private def initializeScript(schema: String) {
    val flyway = new Flyway()

    flyway.setInitOnMigrate(true)
    flyway.setDataSource(dataSource)
    flyway.setLocations(s"db/$schema")
    flyway.setTable(schema + "_version")

    // Do our best to fix any errors:
    flyway.repair()
    flyway.migrate()
  }
}
