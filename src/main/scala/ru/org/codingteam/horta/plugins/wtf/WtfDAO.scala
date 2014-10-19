package ru.org.codingteam.horta.plugins.wtf

import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

class WtfDAO extends DAO {

  override def schema = "wtf"

  override def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any] = {
    val WtfDefinition(_, room, word, definition, author) = obj
    val id = sql"insert into Wtf (room, word, definition, author) values ($room, $word, $definition, $author)"
      .updateAndReturnGeneratedKey().apply()
    Some(id)
  }

  override def read(implicit session: DBSession, id: Any): Option[Any] = {
    val (room: String, word: String) = id
    sql"select id, definition, author from Wtf where room = $room and upper(word) = upper($word)".map(
      rs => WtfDefinition(Some(rs.int("id")), room, word, rs.string("definition"), rs.string("author")))
      .single().apply()
  }

  override def delete(implicit session: DBSession, anyId: Any): Boolean = {
    val id = anyId.asInstanceOf[Int]
    sql"delete from wtf where id = $id".update().apply() == 1
  }

}
