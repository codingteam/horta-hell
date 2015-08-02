package ru.org.codingteam.horta.plugins.karma

import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import scalikejdbc._

case class KarmaRepository(session: DBSession) {

  implicit val s = session

  def getTopKarma(room: String): List[String] = {
    sql"""select top 5 member, karma
          from Karma
          where room = $room
          order by karma desc
       """.map(rs => rs.string("member") + " " + rs.string("karma")).list().apply()
  }

  def getKarma(room: String, member: String): Int = {
    sql"""select karma
        from Karma
        where room = $room and member = $member
     """.map(rs => rs.int("karma")).single().apply().getOrElse(0)
  }

  def getLastChangeTime(room: String, member: String): Option[DateTime] = {
    sql"""select changetime
          from KarmaChanges
          where room = $room and member = $member
       """.map(rs => rs.jodaDateTime("changetime")).single().apply()
  }

  def setKarma(room: String, user: String, member: String, karma: Int): Option[Long] = {
    setLastChangeTime(room, user)

    karmaIsPresentInDB(room, member) match {
      case Some(key: Long) =>
        sql"""update Karma
        set karma=karma + $karma
        where room = $room and member = $member
        """.update().apply()
        Some(key)
      case None =>
        Option(sql"""insert into Karma (room,member,karma)
        values ($room, $member, $karma)
        """.updateAndReturnGeneratedKey().apply())
    }
  }

  private def karmaIsPresentInDB(room: String, member: String): Option[Long] = {
    sql"""select id
    from Karma
    where room = $room and member = $member
    """.map(rs => rs.long(1)).single().apply()
  }

  private def setLastChangeTime(room: String, member: String): Unit = {
    if (changeIsPresentInDB(room, member)) {
      sql"""update KarmaChanges
      set changetime=${Clock.now}
      where room = $room and member = $member
      """.update().apply()
    } else {
      sql"""insert into KarmaChanges (room,member,changetime)
      values ($room, $member, ${Clock.now})
      """.update().apply()
    }
  }

  private def changeIsPresentInDB(room: String, member: String): Boolean = {
    sql"""select exists (select *
    from KarmaChanges
    where room = $room and member = $member)
    """.map(rs => (rs.boolean(1))).single().apply().getOrElse(false)
  }

}
