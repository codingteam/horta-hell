package ru.org.codingteam.horta.plugins.log

import java.sql.Connection
import ru.org.codingteam.horta.database.DAO

/**
 * Data access object for room log storage.
 */
class LogDAO extends DAO {

  override def directoryName: String = "log"

  private abstract sealed case class EventType(name: String)
  private case object EnterType extends EventType("enter")
  private case object LeaveType extends EventType("leave")
  private case object MessageType extends EventType("message")

  /**
   * Store an object in the database.
   * @param connection connection to access the database.
   * @param id object id (if null then it should be generated).
   * @param obj stored object.
   * @return stored object id (or None if object was not stored).
   */
  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] = ???

  /**
   * Delete an object from the database.
   * @param connection connection to access the database.
   * @param id object id.
   * @return true if object was successfully deleted, false otherwise.
   */
  override def delete(connection: Connection, id: Any): Boolean = ???

  /**
   * Read an object from the database.
   * @param connection connection to access the database.
   * @param id object id.
   * @return stored object or None if object not found.
   */
  override def read(connection: Connection, id: Any): Option[Any] = ???

}
