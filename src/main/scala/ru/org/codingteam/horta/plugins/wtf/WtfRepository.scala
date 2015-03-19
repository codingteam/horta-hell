package ru.org.codingteam.horta.plugins.wtf

import scalikejdbc._

case class WtfRepository(session: DBSession) {

  implicit val s = session

  def store(obj: WtfDefinition): Unit = {
    val WtfDefinition(_, room, word, definition, author) = obj
    sql"insert into Wtf (room, word, definition, author) values ($room, $word, $definition, $author)"
      .update().apply()
  }

  def read(room: String, word: String) = {
    sql"select id, definition, author from Wtf where room = $room and upper(word) = upper($word)".map(
      rs => WtfDefinition(Some(rs.int("id")), room, word, rs.string("definition"), rs.string("author")))
      .single().apply()
  }

  def delete(id: Int): Boolean = {
    sql"delete from wtf where id = $id".update().apply() == 1
  }

}
