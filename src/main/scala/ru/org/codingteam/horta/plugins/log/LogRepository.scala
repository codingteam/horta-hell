package ru.org.codingteam.horta.plugins.log

import scalikejdbc._

/**
 * Repository for room log storage.
 */
case class LogRepository(session: DBSession) {

  val MAX_MESSAGES_IN_RESULT = 5

  implicit val s = session

  def store(message: LogMessage): Unit = {
    message match {
      case LogMessage(_, time, room, sender, eventType, text) =>
        sql"""insert into Log (time, room, sender, type, message)
              values ($time, $room, $sender, ${eventType.name}, $text)"""
          .updateAndReturnGeneratedKey().apply()
    }
  }

  def getMessages(room: String, phrase: String): Seq[LogMessage] = {
    val result = sql"""select top $MAX_MESSAGES_IN_RESULT id, time, sender, type, message
          from Log
          where room = $room and message like ${"%" + phrase + "%"}
       """.map(rs => LogMessage(
      Some(rs.int("id")),
      rs.jodaDateTime("time"),
      room,
      rs.string("sender"),
      EventType(rs.string("type")),
      rs.string("message"))).list().apply()
    result
  }

  def getMessagesByUser(room: String, user: String, eventType: EventType): Seq[LogMessage] = {
    sql"""select id, time, type, message
          from Log
          where room = $room and sender = $user
       """.map(rs => LogMessage(
      Some(rs.int("id")),
      rs.jodaDateTime("time"),
      room,
      user,
      EventType(rs.string("type")),
      rs.string("message"))).list().apply()
  }
}
