package ru.org.codingteam.horta.actors.database

import akka.actor.{ActorRef, ActorLogging, Actor}
import akka.pattern.ask
import org.h2.jdbcx.JdbcConnectionPool
import java.sql.Connection
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import concurrent.Await

case class RegisterStore(plugin: String, store: DAO)

case object StoreOkReply

case class StoreObject(plugin: String, id: Option[Any], obj: Any)

case class ReadObject(plugin: String, id: Any)

trait DAO {
	def initializeTable(connection: Connection)

	def isTableInitialized(connection: Connection): Boolean

	def store(connection: Connection, id: Option[Any], obj: Any): Any

	def read(connection: Connection, id: Any): Option[Any]
}

class PersistentStore() extends Actor with ActorLogging {

	import context.dispatcher

	implicit val timeout = Timeout(60 seconds)

	var connection: Connection = null
	var daos = Map[String, DAO]()

	override def preStart() {
		val pool = JdbcConnectionPool.create("jdbc:h2:hell;DB_CLOSE_DELAY=-1", "sa", "")
		connection = pool.getConnection()
	}

	def receive() = {
		case RegisterStore(plugin, dao) => {
			daos += plugin -> dao
		}

		case StoreObject(plugin, id, obj) => {
			daos.get(plugin) match {
				case Some(dao) => {
					if (!dao.isTableInitialized(connection)) {
						dao.initializeTable(connection)
					}
					dao.store(connection, id, obj)
					sender ! StoreOkReply
				}

				case None => {
					log.info(s"Cannot store object $obj for plugin $plugin")
				}
			}
		}

		case ReadObject(plugin, id) => {
			daos.get(plugin) match {
				case Some(dao) => {
					if (!dao.isTableInitialized(connection)) {
						dao.initializeTable(connection)
					}
					sender ! dao.read(connection, id)
				}

				case None => {
					log.info(s"Cannot read object $id for plugin $plugin")
				}
			}
		}
	}
}
