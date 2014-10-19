package ru.org.codingteam.horta.plugins.mail

import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

class MailDAO extends DAO {

  override def schema = "mail"

  /**
   * Store new message in a database.
   * @param session session to access the database.
   * @param id object id (always None).
   * @param obj a MailMessage instance.
   * @return stored object id (or None if object was not stored).
   */
  override def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any] = {
    val MailMessage(_, room, senderNick, receiverNick, text) = obj
    val id = sql"insert into Mail (room, sender, receiver, message) values ($room, $senderNick, $receiverNick, $text)"
      .updateAndReturnGeneratedKey().apply()
    Some(id)
  }

  /**
   * Read the messages from the database.
   * @param session session to access the database.
   * @param id tuple (room, receiverNick).
   * @return stored message sequence.
   */
  override def read(implicit session: DBSession, id: Any): Option[Any] = {
    val (room: String, receiverNick: String) = id
    val result = sql"select id, sender, message from Mail where room = $room and receiver = $receiverNick".map(
      rs => MailMessage(Some(rs.int("id")), room, rs.string("sender"), receiverNick, rs.string("message")))
      .list().apply()
    Some(result)
  }

  /**
   * Delete the message from the database.
   * @param session session to access the database.
   * @param anyId object id.
   * @return true if object was successfully deleted, false otherwise.
   */
  override def delete(implicit session: DBSession, anyId: Any): Boolean = {
    val id = anyId.asInstanceOf[Int]
    sql"delete from mail where id = $id".update().apply() == 1
  }

}
