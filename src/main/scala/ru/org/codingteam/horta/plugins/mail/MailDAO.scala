package ru.org.codingteam.horta.plugins.mail

import ru.org.codingteam.horta.database.DAO
import java.sql.Connection

class MailDAO extends DAO {
  override def directoryName: String = ???

  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] = ???

  override def read(connection: Connection, id: Any): Option[Any] = ???

  override def delete(connection: Connection, id: Any): Boolean = ???
}
