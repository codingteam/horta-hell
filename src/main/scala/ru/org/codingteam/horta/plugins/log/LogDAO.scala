package ru.org.codingteam.horta.plugins.log

import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

case class GetMessages(room: String, phrase: String)

/**
 * Data access object for room log storage.
 */
class LogDAO extends DAO {

  val MAX_MESSAGES_IN_RESULT = 5

  override def schema: String = "log"

  /**
   * Store an object in the database.
   * @param session session to access the database.
   * @param id object id (if null then it should be generated).
   * @param obj stored object.
   * @return stored object id (or None if object was not stored).
   */
  override def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any] = {
    val LogMessage(_, time, room, sender, eventType, text) = obj
    val id = sql"""insert into Log (time, room, sender, type, message)
                values ($time, $room, $sender, ${eventType.name}, $text)"""
      .updateAndReturnGeneratedKey().apply()
    Some(id)
  }

  /**
   * Delete an object from the database.
   * @param session session to access the database.
   * @param id object id.
   * @return true if object was successfully deleted, false otherwise.
   */
  override def delete(implicit session: DBSession, id: Any): Boolean = ???

  /**
   * Read an object from the database.
   * @param session session to access the database.
   * @param id object id.
   * @return stored object or None if object not found.
   */
  override def read(implicit session: DBSession, id: Any): Option[Any] = {
    id match {
      case GetMessages(room, phrase) =>
        queryRoomMessages(session, room, phrase)
    }
  }

  private def queryRoomMessages(implicit session: DBSession, room: String, phrase: String): Option[Seq[LogMessage]] = {
    val result = sql"""select top $MAX_MESSAGES_IN_RESULT id, time, sender, type, message
          from Log
          where room = $room and message like ${"%" + phrase + "%"}
       """.map(rs => LogMessage(
      Some(rs.int("id")),
      rs.jodaDateTime("time"),
      room,
      rs.string("sender"),
      EventType(rs.string("type")),
      rs.string("message"))).list().apply()
    Some(result)
  }

}
