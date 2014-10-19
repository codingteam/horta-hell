package ru.org.codingteam.horta.plugins.pet

import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.database.DAO
import scalikejdbc._

case class PetDataId(room: String)
case class PetCoinsId(room: String)
case class PetCoinTransactionsId(room: String, nick: String)

case class PetCoinTransaction(name: String, state1: Map[String, Int], state2: Map[String, Int])
case class PetCoinTransactionModel(id: Int, room: String, nickname: String, time: DateTime, change: Int, reason: String)

class PetDAO extends DAO {

  val transactionViewLimit = 10

  override def schema = "pet"

  override def store(implicit session: DBSession, id: Option[Any], obj: Any): Option[Any] = {
    (id, obj) match {
      case (Some(PetDataId(room)), data: PetData) => storePetData(session, room, data)
      case (Some(PetCoinsId(room)), transaction: PetCoinTransaction) =>
        storeTransaction(session, room, transaction)
        Some(Unit)
      case _ => sys.error(s"Invalid parameters for the PetDAO.store: $id, $obj")
    }
  }

  override def read(implicit session: DBSession, id: Any): Option[Any] = {
    id match {
      case PetDataId(roomName) => readPetData(session, roomName)
      case PetCoinsId(roomName) => Some(readCoins(session, roomName))
      case PetCoinTransactionsId(roomName, nickname) => Some(readPetCoinTransactions(session, roomName, nickname))
    }
  }

  override def delete(implicit session: DBSession, id: Any): Boolean = false

  private def storePetData(implicit session: DBSession, room: String, data: PetData) = {
    val roomName = room.asInstanceOf[String]
    val exist = sql"select * from Pet where room = $roomName".map(rs => true).single().apply()
    exist match {
      case Some(_) => updatePetData(session, roomName, data)
      case None => insertPetData(session, roomName, data)
    }

    Some(Unit)
  }

  private def readPetData(implicit session: DBSession, roomName: String) = {
    sql"select * from Pet where room = $roomName".map(
      rs => PetData(
        rs.string("nickname"),
        rs.boolean("alive"),
        rs.int("health"),
        rs.int("satiation"),
        rs.jodaDateTime("birth"))).single().apply()
  }

  private def readCoins(implicit session: DBSession, room: String): Map[String, Int] = {
    sql"select nick, amount from PetCoins where room = $room".map(
      rs => rs.string("nick") -> rs.int("amount")).list().apply().toMap
  }

  private def readPetCoinTransactions(implicit session: DBSession, room: String, nickname: String) = {
    sql"""
       select id, time, change, reason
       from PetTransaction
       where room = $room and nickname = $nickname
       order by time desc
       limit $transactionViewLimit
      """.map(
        rs => PetCoinTransactionModel(
          rs.int("id"),
          room,
          nickname,
          rs.jodaDateTime("time"),
          rs.int("change"),
          rs.string("reason"))).list().apply()
  }

  private def storeTransaction(session: DBSession, roomName: String, data: PetCoinTransaction): Unit = {
    val PetCoinTransaction(transactionName, state1, state2) = data
    storeTransactionHistory(session, roomName, Clock.now, transactionName, state1, state2)
    deleteCoins(session, roomName)
    insertCoins(session, roomName, state2)
  }

  private def storeTransactionHistory(implicit session: DBSession,
                                      roomName: String,
                                      time: DateTime,
                                      transactionName: String,
                                      state1: Map[String, Int],
                                      state2: Map[String, Int]): Unit = {
    val keys = state1.keySet.union(state2.keySet)
    val values = for (key <- keys) yield (key, state1.getOrElse(key, 0), state2.getOrElse(key, 0))
    val diff = values.map {
      case (key, value1, value2) => (key, value2 - value1)
    }.filter(_._2 != 0).toMap

    diff.foreach {
      case (nick, change) =>
        sql"""insert into PetTransaction (room, nickname, time, change, reason)
              values ($roomName, $nick, $time, $change, $transactionName)""".update().apply()
    }
  }

  private def insertPetData(implicit session: DBSession, room: String, obj: PetData) {
    val PetData(nickname, alive, health, satiation, birth) = obj
    sql"""insert into Pet (room, nickname, alive, health, satiation, birth)
          values ($room, $nickname, $alive, $health, $satiation, $birth)""".update().apply()
  }

  private def updatePetData(implicit session: DBSession, room: String, obj: PetData) {
    val PetData(nickname, alive, health, satiation, birth) = obj
    sql"""update Pet
          set nickname = $nickname, alive = $alive, health = $health, satiation = $satiation, birth = $birth
          where room = $room""".update().apply()
  }

  private def insertCoins(implicit session: DBSession, room: String, coins: Map[String, Int]) {
    coins filter (_._2 > 0) foreach {
      case (nick, amount) =>
        sql"insert into PetCoins(room, nick, amount) values ($room, $nick, $amount)".update().apply()
    }
  }

  private def deleteCoins(implicit session: DBSession, room: String) {
    sql"delete from PetCoins where room = $room".update().apply()
  }

}
