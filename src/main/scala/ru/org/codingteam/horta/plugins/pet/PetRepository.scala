package ru.org.codingteam.horta.plugins.pet

import org.joda.time.DateTime
import ru.org.codingteam.horta.core.Clock
import scalikejdbc._

case class PetCoinTransaction(name: String, state1: Map[String, Int], state2: Map[String, Int])
case class PetCoinTransactionModel(id: Int, room: String, nickname: String, time: DateTime, change: Int, reason: String)

case class PetRepository(session: DBSession) {

  val transactionViewLimit = 10

  implicit val s = session

  def storePetData(room: String, data: PetData): Unit = {
    val exist = sql"select * from Pet where room = $room".map(rs => true).single().apply()
    exist match {
      case Some(_) => updatePetData(room, data)
      case None => insertPetData(room, data)
    }
  }

  def readPetData(roomName: String) = {
    sql"select * from Pet where room = $roomName".map(
      rs => PetData(
        rs.string("nickname"),
        rs.boolean("alive"),
        rs.int("health"),
        rs.int("satiation"),
        rs.jodaDateTime("birth"))).single().apply()
  }

  def readCoins(room: String): Map[String, Int] = {
    sql"select nick, amount from PetCoins where room = $room".map(
      rs => rs.string("nick") -> rs.int("amount")).list().apply().toMap
  }

  def storeTransaction(roomName: String, data: PetCoinTransaction): Unit = {
    val PetCoinTransaction(transactionName, state1, state2) = data
    storeTransactionHistory(roomName, Clock.now, transactionName, state1, state2)
    deleteCoins(roomName)
    insertCoins(roomName, state2)
  }

  def readTransactions(room: String, nickname: String): Seq[PetCoinTransactionModel] = {
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

  private def updatePetData(room: String, obj: PetData) {
    val PetData(nickname, alive, health, satiation, birth) = obj
    sql"""update Pet
          set nickname = $nickname, alive = $alive, health = $health, satiation = $satiation, birth = $birth
          where room = $room""".update().apply()
  }

  private def insertPetData(room: String, obj: PetData) {
    val PetData(nickname, alive, health, satiation, birth) = obj
    sql"""insert into Pet (room, nickname, alive, health, satiation, birth)
          values ($room, $nickname, $alive, $health, $satiation, $birth)""".update().apply()
  }

  private def storeTransactionHistory(roomName: String,
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

  private def insertCoins(room: String, coins: Map[String, Int]) {
    coins filter (_._2 > 0) foreach {
      case (nick, amount) =>
        sql"insert into PetCoins(room, nick, amount) values ($room, $nick, $amount)".update().apply()
    }
  }

  private def deleteCoins(room: String) {
    sql"delete from PetCoins where room = $room".update().apply()
  }

}
