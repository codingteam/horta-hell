package ru.org.codingteam.horta.plugins.wtf

import java.sql.{Connection, Statement}
import ru.org.codingteam.horta.database.DAO

class WtfDAO extends DAO {
  override def directoryName = "wtf"

  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] =
    obj match {
      case WtfDefinition(_, room, word, definition, author) => {
        val query = connection.prepareStatement(
          """
            |insert into wtf (room, word, definition, author)
            |values (?, ?, ?, ?)
          """.stripMargin, Statement.RETURN_GENERATED_KEYS)
        try {
          query.setString(1, room)
          query.setString(2, word)
          query.setString(3, definition)
          query.setString(4, author)

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

  override def read(connection: Connection, id: Any): Option[Any] =
    id match {
      case (room: String, word: String) => {
        val query = connection.prepareStatement(
          """
            |select id, definition, author
            |from wtf
            |where room = ? and UPPER(word) = UPPER(?)
          """.stripMargin)
        try {
          query.setString(1, room)
          query.setString(2, word)
          val resultSet = query.executeQuery()
          try {
            if (resultSet.next()) {
              val id = resultSet.getInt("id")
              val definition = resultSet.getString("definition")
              val author = resultSet.getString("author")
              Some(WtfDefinition(Some(id), room, word, definition, author))
            } else {
              None
            }
          } finally {
            resultSet.close()
          }
        } finally {
          query.close()
        }
      }
    }

  override def delete(connection: Connection, id: Any): Boolean =
    id match {
      case id: Int => {
        val query = connection.prepareStatement("delete from wtf where id = ?")
        try {
          query.setInt(1, id)
          query.executeUpdate() == 1
        } finally {
          query.close()
        }
      }
    }
}
