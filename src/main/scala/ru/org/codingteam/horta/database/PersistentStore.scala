package ru.org.codingteam.horta.database

import javax.sql.DataSource

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import com.googlecode.flyway.core.Flyway
import org.h2.jdbcx.JdbcConnectionPool
import ru.org.codingteam.horta.configuration.Configuration
import scalikejdbc.{ConnectionPool, DB, DBSession, DataSourceConnectionPool}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

class PersistentStore(repositories: Map[String, Repository]) extends Actor with ActorLogging {

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
    case PersistentStore.Execute(plugin, action) =>
      repositories.get(plugin) match {
        case Some(repository) =>
          initializeDatabase(repository)
          withTransaction { session =>
            sender ! action(repository, session)
          }

        case None =>
          log.info(s"Cannot execute action $action for plugin $plugin: repository not found")
      }
  }

  private def initializeDatabase(repository: Repository) {
    val schema = repository.schema
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

object PersistentStore {

  private case class Execute(plugin: String, action: (Repository, DBSession) => Any)

  def execute[Repository, T: ClassTag](plugin: String, store: ActorRef)
                            (action: (Repository, DBSession) => T)
                            (implicit timeout: Timeout): Future[T] = {
    val message = Execute(plugin, (r, s) => action(r.asInstanceOf[Repository], s))
    (store ? message).mapTo[T]
  }

}
