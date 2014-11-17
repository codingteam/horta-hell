package ru.org.codingteam.horta.plugins.karma

import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

case class SetKarma(room: String, member: String, karma: Int)
case class GetKarma(room: String, member: String)
case class GetTopKarma(room: String)

class KarmaDAO extends DAO {

  val PERIOD_BETWEEN_CHANGES = 60*60

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
      case Some(SetKarma(room, member, karma)) =>
        querySetKarma(session, room, member, karma)
      case _ => sys.error("Unknown argument for KarmaDAO.store")
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
      case GetTopKarma(room) =>
        queryTopKarma(session, room)
      case _ => sys.error("Unknown argument for KarmaDAO.read")
    }
  }

  private def queryKarma(implicit session: DBSession, room: String, member: String): Option[Int] = {
    val result = sql"""select karma
          from Karma
          where room = $room and member = $member
       """.map(rs => rs.int("karma")).single().apply()
    result
  }

  private def queryTopKarma(implicit session: DBSession, room: String): Option[List[String]] = { // DON'T CHANGE SIGNATURE, CAST SOMEWHERE!
    val result = sql"""select top 5 member, karma
          from Karma
          where room = $room
          order by karma desc
       """.map(rs => rs.string("member") + " " + rs.string("karma")).list().apply()
    Option(result)
  }

  private def queryIsPresentInDB(implicit session: DBSession, room: String, member: String):Boolean = {
    val res = sql"""select exists (select *
    from Karma
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(1))).single().apply()
    res.getOrElse(false)
  }

  private def querySetKarma(implicit session: DBSession, room: String, member: String, karma: Int): Option[Any] = {
    if (queryIsPresentInDB(session, room, member)) {
      val prev_karma = queryKarma(session, room, member) match {
        case Some(prev:Int) =>
          prev
        case _ =>
          0
      }
      val resp = sql"""update Karma
      set karma=${karma + prev_karma}
      where room = $room and member = $member
      """.update().apply()
      Some(resp)
    } else {
      val resp = sql"""insert into Karma (room,member,karma)
      values ($room, $member, $karma)
      """.update().apply()
      Some(resp)
    }
  }
}
