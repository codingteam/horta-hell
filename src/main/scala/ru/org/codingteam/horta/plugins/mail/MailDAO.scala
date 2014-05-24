package ru.org.codingteam.horta.plugins.mail

import java.sql.{Connection, Statement}
import ru.org.codingteam.horta.database.DAO

class MailDAO extends DAO {

  override def directoryName = "mail"

  /**
   * Store new message in a database.
   * @param connection connection to access the database.
   * @param id object id (always None).
   * @param obj a MailMessage instance.
   * @return stored object id (or None if object was not stored).
   */
  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] = {
    obj match {
      case MailMessage(_, room, senderNick, receiverNick, text) =>
        val query = connection.prepareStatement(
          """
            |insert into mail (room, sender, receiver, message)
            |values (?, ?, ?, ?)
          """.stripMargin, Statement.RETURN_GENERATED_KEYS)
        try {
          query.setString(1, room)
          query.setString(2, senderNick)
          query.setString(3, receiverNick)
          query.setString(4, text)

          query.executeUpdate()
          val resultSet = query.getGeneratedKeys
          try {
            resultSet.next()
            Some(resultSet.getInt(1))
          } finally {
            resultSet.close()
          }
        } finally {
          query.close()
        }
    }
  }

  /**
   * Read the messages from the database.
   * @param connection connection to access the database.
   * @param id tuple (room, receiverNick).
   * @return stored message sequence.
   */
  override def read(connection: Connection, id: Any): Option[Any] = {
    id match {
      case (room: String, receiverNick: String) =>
        val query = connection.prepareStatement(
          """
            |select id, sender, message
            |from mail
            |where room = ? and receiver = ?
          """.stripMargin)
        try {
          query.setString(1, room)
          query.setString(2, receiverNick)
          val resultSet = query.executeQuery()
          try {
            var result = List[MailMessage]()
            while (resultSet.next()) {
              val id = resultSet.getInt("id")
              val sender = resultSet.getString("sender")
              val message = resultSet.getString("message")
              result :+= MailMessage(Some(id), room, sender, receiverNick, message)
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

  /**
   * Delete the message from the database.
   * @param connection connection to access the database.
   * @param id object id.
   * @return true if object was successfully deleted, false otherwise.
   */
  override def delete(connection: Connection, id: Any): Boolean = {
    id match {
      case id: Int =>
        val query = connection.prepareStatement(
          """
            |delete from mail where id = ?
          """.stripMargin)
        try {
          query.setInt(1, id)
          query.executeUpdate() == 1
        } finally {
          query.close()
        }
    }
  }

}
