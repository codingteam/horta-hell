package ru.org.codingteam.horta.plugins.log

import java.sql.{Connection, Statement, Timestamp}
import ru.org.codingteam.horta.database.DAO

/**
 * Data access object for room log storage.
 */
class LogDAO extends DAO {

  override def directoryName: String = "log"

  /**
   * Store an object in the database.
   * @param connection connection to access the database.
   * @param id object id (if null then it should be generated).
   * @param obj stored object.
   * @return stored object id (or None if object was not stored).
   */
  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] = {
    val LogMessage(_, time, room, sender, eventType, text) = obj
    val query = connection.prepareStatement(
      """
        |insert into log (time, room, sender, type, message)
        |values (?, ?, ?, ?, ?)
      """.stripMargin, Statement.RETURN_GENERATED_KEYS)
    try {
      query.setTimestamp(1, new Timestamp(time.getMillis))
      query.setString(2, room)
      query.setString(3, sender)
      query.setString(4, eventType.name)
      query.setString(5, text)

      query.executeUpdate()
      val resultSet = query.getGeneratedKeys
      try {
        resultSet.next()
        Some(resultSet.getLong(1))
      } finally {
        resultSet.close()
      }
    } finally {
      query.close()
    }
  }

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
