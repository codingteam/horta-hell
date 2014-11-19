package ru.org.codingteam.horta.plugins.karma

import java.sql.Date

import org.joda.time.{DateTime, Period}
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

case class SetKarma(room: String, user:String, member: String, karma: Int)
case class GetKarma(room: String, member: String)
case class GetTopKarma(room: String)
case class GetLastChangeTime(room: String, user:String)

class KarmaDAO extends DAO {

  override def schema: String = "Karma"

  override def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any] = {
    id match {
      case Some(SetKarma(room, user, member, karma)) =>
        querySetKarma(session, room, user, member, karma)
      case _ => sys.error("Unknown argument for KarmaDAO.store")
    }
  }

  override def delete(implicit session: DBSession, id: Any): Boolean = ???

  override def read(implicit session: DBSession, id: Any): Option[Any] = {
    id match {
      case GetKarma(room, member) =>
        queryKarma(session, room, member)
      case GetTopKarma(room) =>
        queryTopKarma(session, room)
      case GetLastChangeTime(room, user) =>
        queryLastChange(session, room, user)
      case _ => sys.error("Unknown argument for KarmaDAO.read")
    }
  }

  private def queryLastChange(implicit session: DBSession, room: String, member: String): Option[DateTime] = {
    sql"""select changetime
          from KarmaChanges
          where room = $room and member = $member
       """.map(rs => rs.jodaDateTime("changetime")).single().apply()
  }

  private def queryChangeIsPresentInDB(implicit session: DBSession, room: String, member: String): Boolean = {
    val res = sql"""select exists (select *
    from KarmaChanges
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(1))).single().apply()
    res.getOrElse(false)
  }

  private def querySetLastChange(implicit session: DBSession, room: String, member: String): Option[Long] = {
    val resp = if (queryChangeIsPresentInDB(session, room, member)) {
      sql"""update KarmaChanges
      set changetime=${Clock.now}
      where room = $room and member = $member
      """.update().apply()
    } else {
      sql"""insert into KarmaChanges (room,member,changetime)
      values ($room, $member, ${Clock.now})
      """.update().apply()
    }
    Option(resp)
  }

  private def queryKarma(implicit session: DBSession, room: String, member: String): Option[Int] = {
    sql"""select karma
        from Karma
        where room = $room and member = $member
     """.map(rs => rs.int("karma")).single().apply()
  }

  private def queryTopKarma(implicit session: DBSession, room: String): Option[List[String]] = {
    val result = sql"""select top 5 member, karma
          from Karma
          where room = $room
          order by karma desc
       """.map(rs => rs.string("member") + " " + rs.string("karma")).list().apply()
    Option(result)
  }

  private def queryKarmaIsPresentInDB(implicit session: DBSession, room: String, member: String): Boolean = {
    val res = sql"""select exists (select *
    from Karma
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(1))).single().apply()
    res.getOrElse(false)
  }

  private def querySetKarma(implicit session: DBSession, room: String, user: String, member: String, karma: Int): Option[Long] = {
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
  }
}
