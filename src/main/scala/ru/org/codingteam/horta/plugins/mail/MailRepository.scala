package ru.org.codingteam.horta.plugins.mail

import scalikejdbc._

case class MailRepository(session: DBSession) {

  implicit val s = session

  def store(message: MailMessage): Unit = {
    message match {
      case MailMessage(_, room, senderNick, receiverNick, text) =>
        sql"insert into Mail (room, sender, receiver, message) values ($room, $senderNick, $receiverNick, $text)"
          .updateAndReturnGeneratedKey().apply()
    }
  }

  def getMessages(room: String, receiverNick: String): Seq[MailMessage] = {
    sql"select id, sender, message from Mail where room = $room and receiver = $receiverNick".map(
      rs => MailMessage(Some(rs.int("id")), room, rs.string("sender"), receiverNick, rs.string("message")))
      .list().apply()
  }

  def deleteMessage(id: Int): Unit = {
    sql"delete from mail where id = $id".update().apply()
  }

}
