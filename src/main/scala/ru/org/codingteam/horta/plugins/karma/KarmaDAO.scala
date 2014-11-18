package ru.org.codingteam.horta.plugins.karma

import java.sql.Date

import org.joda.time.{DateTime, Period}
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

case class SetKarma(room: String, user:String, member: String, karma: Int)
case class GetKarma(room: String, member: String)
case class GetTopKarma(room: String)

class KarmaDAO extends DAO {

  val PERIOD_BETWEEN_CHANGES = 3 // hours

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
      case Some(SetKarma(room, user, member, karma)) =>
        querySetKarma(session, room, user, member, karma)
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

  private def queryLastChange(implicit session: DBSession, room: String, member: String):Option[DateTime] = {
    sql"""select changetime
          from KarmaChanges
          where room = $room and member = $member
       """.map(rs => rs.jodaDateTime("changetime")).single().apply()
  }

  private def queryChangeIsPresentInDB(implicit session: DBSession, room: String, member: String):Boolean = {
    val res = sql"""select exists (select *
    from KarmaChanges
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(1))).single().apply()
    res.getOrElse(false)
  }

  private def querySetLastChange(implicit session: DBSession, room: String, member: String):Option[Any] = {
    val resp = if (queryChangeIsPresentInDB(session, room, member)) {
      sql"""update KarmaChanges
      set changetime=${Clock.now}
      where room = $room and member = $member
      """.update().apply()
    } else {
      val resp = sql"""insert into KarmaChanges (room,member,changetime)
      values ($room, $member, ${Clock.now})
      """.update().apply()
    }
    Option(resp)
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

  private def queryKarmaIsPresentInDB(implicit session: DBSession, room: String, member: String):Boolean = {
    val res = sql"""select exists (select *
    from Karma
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(1))).single().apply()
    res.getOrElse(false)
  }

  private def querySetKarma(implicit session: DBSession, room: String, user: String, member: String, karma: Int): Option[Any] = {
    val canChange = queryLastChange(session, room, user) match {
      case None => true
      case Some(time:DateTime) => new Period(time, Clock.now).toDurationFrom(DateTime.now).toStandardHours.getHours > PERIOD_BETWEEN_CHANGES
      case _ => sys.error("Unknown return value for KarmaDAO.queryLastChange")
    }

    if (canChange) {
      querySetLastChange(session, room, user)
      if (queryKarmaIsPresentInDB(session, room, member)) {
        val prev_karma = queryKarma(session, room, member) match {
          case Some(prev: Int) =>
            prev
          case _ =>
            sys.error("queryKarmaIsPresentInDB in KarmaDAO.querySetKarma must return actual info")
        }
        val resp = sql"""update Karma
          set karma=${karma + prev_karma}
          where room = $room and member = $member
          """.update().apply()
        Option(resp)
      } else {
        val resp = sql"""insert into Karma (room,member,karma)
      values ($room, $member, $karma)
      """.update().apply()
        Option(resp)
      }
    } else {
      None
    }
  }
}
