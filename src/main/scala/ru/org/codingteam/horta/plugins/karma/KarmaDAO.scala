package ru.org.codingteam.horta.plugins.karma

import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

case class SetKarma(room: String, member: String, karma: Int)
case class GetKarma(room: String, member: String)
case class GetTopKarma(room: String, order: Order)

class KarmaDAO extends DAO {

  val MAX_MESSAGES_IN_RESULT = 5

  /**
   * Schema name for current DAO.
   * @return schema name.
   */
  override def schema: String = "Karma"

  /**
   * Store an object in the database.
   * @param session session to access the database.
   * @param id object id (if null then it should be generated).
   * @param obj stored object.
   * @return stored object id (or None if object was not stored).
   */
  override def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any] = {
    id match {
      case SetKarma(room, member, karma) =>
        querySetKarma(session, room, member, karma)
      case _ => Option[Any]()
    }
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
      case GetKarma(room, member) =>
        queryKarma(session, room, member)
      case GetTopKarma(room, order) =>
        queryTopKarma(session, room, order)
      case _ => Option[Any]()
    }
  }

  private def queryKarma(implicit session: DBSession, room: String, member: String): Option[Int] = {
    val result = sql"""select karma
          from $schema
          where room = $room and member = $member
       """.map(rs => rs.int("karma")).single().apply()
    result
  }

  private def queryTopKarma(implicit session: DBSession, room: String, order: Order): Option[List[String]] = {
    val result = sql"""select top $MAX_MESSAGES_IN_RESULT member, karma
          from $schema
          where room = $room
          ordered by karma ${order.order}
       """.map(rs => rs.string("member") + rs.string("karma")).list().apply()
    Option(result)
  }

  private def queryIsPresentInDB(room: String, member: String):Boolean = {
    val res = sql"""select exists (select *
    from $schema
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(0))).single().apply()
    res.getOrElse(false)
  }

  private def querySetKarma(session: DBSession, room: String, member: String, karma: Int): Option[Any] = {
    if (queryIsPresentInDB(room, member)) {
      val resp = sql"""update $schema
      set karma=$karma
      where where room = $room and member = $member
      """.update().apply()
      Some(resp)
    } else {
      val resp = sql"""insert into $schema
      values $room, $member, $karma
      """.update().apply()
      Some(resp)
    }
  }
}
