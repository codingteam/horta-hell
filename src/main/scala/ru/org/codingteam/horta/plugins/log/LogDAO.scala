package ru.org.codingteam.horta.plugins.log

import java.sql.{Connection, Statement, Timestamp}
import org.joda.time.DateTime
import ru.org.codingteam.horta.database.DAO

case class GetMessages(room: String, phrase: String)

/**
 * Data access object for room log storage.
 */
class LogDAO extends DAO {

  val MAX_MESSAGES_IN_RESULT = 5

  override def schema: String = "log"

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
  override def read(connection: Connection, id: Any): Option[Any] = {
    id match {
      case GetMessages(room, phrase) =>
        queryRoomMessages(connection, room, phrase)
    }
  }

  private def queryRoomMessages(connection: Connection, room: String, phrase: String): Option[Seq[LogMessage]] = {
    val query = connection.prepareStatement(
      """
        |select top(?) id, time, sender, type, message
        |from log
        |where message like ?
      """.stripMargin)
    try {
      query.setInt(1, MAX_MESSAGES_IN_RESULT)
      query.setString(2, s"%$phrase%")

      var result = List[LogMessage]()
      val resultSet = query.executeQuery()
      try {
        while (resultSet.next()) {
          val id = Some(resultSet.getInt("id"))
          val time = new DateTime(resultSet.getTimestamp("time").getTime)
          val sender = resultSet.getString("sender")
          val eventType = EventType(resultSet.getString("type"))
          val text = resultSet.getString("message")

          val message = LogMessage(id, time, room, sender, eventType, text)
          result +:= message
        }

        Some(result)
      } finally {
        resultSet.close()
      }
    } finally {
      query.close()
    }
  }

}
