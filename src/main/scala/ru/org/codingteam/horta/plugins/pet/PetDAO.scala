package ru.org.codingteam.horta.plugins.pet

import java.sql.Connection

import org.joda.time.{DateTime, DateTimeZone}
import ru.org.codingteam.horta.core.Clock
import ru.org.codingteam.horta.database.DAO
import ru.org.codingteam.horta.database.DateTimeConverter._

case class PetDataId(room: String)
case class PetCoinsId(room: String)
case class PetCoinTransactionsId(room: String, nick: String)

case class PetCoinTransaction(name: String, state1: Map[String, Int], state2: Map[String, Int])
case class PetCoinTransactionModel(id: Int, room: String, nickname: String, time: DateTime, change: Int, reason: String)

class PetDAO extends DAO {

  val transactionViewLimit = 10

  override def schema = "pet"

  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] = {
    (id, obj) match {
      case (Some(PetDataId(room)), data: PetData) => storePetData(connection, room, data)
      case (Some(PetCoinsId(room)), transaction: PetCoinTransaction) =>
        storeTransaction(connection, room, transaction)
        Some(Unit)
      case _ => sys.error(s"Invalid parameters for the PetDAO.store: $id, $obj")
    }
  }

  override def read(connection: Connection, id: Any): Option[Any] = {
    id match {
      case PetDataId(roomName) => readPetData(connection, roomName)
      case PetCoinsId(roomName) => Some(readCoins(connection, roomName))
      case PetCoinTransactionsId(roomName, nickname) => Some(readPetCoinTransactions(connection, roomName, nickname))
    }
  }

  override def delete(connection: Connection, id: Any): Boolean = false

  private def storePetData(connection: Connection, room: String, data: PetData) = {
    val roomName = room.asInstanceOf[String]
    val select = connection.prepareStatement("SELECT * FROM pet WHERE room = ?")
    try {
      select.setString(1, roomName)
      val resultSet = select.executeQuery()
      try {
        if (resultSet.next()) {
          updatePetData(connection, roomName, data)
        } else {
          insertPetData(connection, roomName, data)
        }

        Some(Unit)
      } finally {
        resultSet.close()
      }
    } finally {
      select.close()
    }
  }

  private def readPetData(connection: Connection, roomName: String) = {
    val select = connection.prepareStatement("SELECT * FROM pet WHERE room = ?")
    try {
      select.setString(1, roomName)
      val resultSet = select.executeQuery()
      try {
        if (resultSet.next()) {
          Some(
            PetData(
              resultSet.getString("nickname"),
              resultSet.getBoolean("alive"),
              resultSet.getInt("health"),
              resultSet.getInt("satiation"),
              new DateTime(resultSet.getTimestamp("birth"), DateTimeZone.UTC)))
        } else {
          None
        }
      } finally {
        resultSet.close()
      }
    } finally {
      select.close()
    }
  }

  private def readCoins(connection: Connection, room: String): Map[String, Int] = {
    val statement = connection.prepareStatement(
      """
        |select nick, amount
        |from PetCoins
        |where room = ?
      """.stripMargin)
    try {
      statement.setString(1, room)
      val resultSet = statement.executeQuery()
      var result = Map[String, Int]()
      while (resultSet.next()) {
        val nick = resultSet.getString("nick")
        val amount = resultSet.getInt("amount")
        result += nick -> amount
      }

      result
    } finally {
      statement.close()
    }
  }

  private def readPetCoinTransactions(connection: Connection, room: String, nickname: String) = {
    val statement = connection.prepareStatement(
      """
        |select id, time, change, reason
        |from PetTransaction
        |where room = ? and nickname = ?
        |order by time desc
        |limit ?
      """.stripMargin)
    try {
      statement.setString(1, room)
      statement.setString(2, nickname)
      statement.setInt(3, transactionViewLimit)
      val resultSet = statement.executeQuery()
      var result = List[PetCoinTransactionModel]()
      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        val time = resultSet.getTimestamp("time")
        val change = resultSet.getInt("change")
        val reason = resultSet.getString("reason")

        result :+= PetCoinTransactionModel(id, room, nickname, time, change, reason)
      }

      result
    } finally {
      statement.close()
    }
  }

  private def storeTransaction(connection: Connection, roomName: String, data: PetCoinTransaction): Unit = {
    val PetCoinTransaction(transactionName, state1, state2) = data
    storeTransactionHistory(connection, roomName, Clock.now, transactionName, state1, state2)
    deleteCoins(connection, roomName)
    insertCoins(connection, roomName, state2)
  }

  private def storeTransactionHistory(connection: Connection,
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

    val statement = connection.prepareStatement(
      """
        |insert into PetTransaction (room, nickname, time, change, reason)
        |values (?, ?, ?, ?, ?)
      """.stripMargin)
    try {
      diff.foreach {
        case (nick, change) =>
          statement.setString(1, roomName)
          statement.setString(2, nick)
          statement.setTimestamp(3, time)
          statement.setInt(4, change)
          statement.setString(5, transactionName)
          statement.executeUpdate()
      }
    } finally {
      statement.close()
    }
  }

  private def insertPetData(connection: Connection, room: String, obj: PetData) {
    val PetData(nickname, alive, health, satiation, birth) = obj
    val statement = connection.prepareStatement(
      "INSERT INTO pet (room, nickname, alive, health, satiation, birth) VALUES (?, ?, ?, ?, ?, ?)")
    try {
      statement.setString(1, room)
      statement.setString(2, nickname)
      statement.setBoolean(3, alive)
      statement.setInt(4, health)
      statement.setInt(5, satiation)
      statement.setTimestamp(6, birth)
      statement.executeUpdate()
    } finally {
      statement.close()
    }
  }

  private def updatePetData(connection: Connection, room: String, obj: PetData) {
    val PetData(nickname, alive, health, satiation, birth) = obj
    val statement = connection.prepareStatement(
      "UPDATE pet SET nickname = ?, alive = ?, health = ?, satiation = ?, birth = ? WHERE room = ?")
    try {
      statement.setString(1, nickname)
      statement.setBoolean(2, alive)
      statement.setInt(3, health)
      statement.setInt(4, satiation)
      statement.setTimestamp(5, birth)
      statement.setString(6, room)
      statement.executeUpdate()
    } finally {
      statement.close()
    }
  }

  private def insertCoins(connection: Connection, room: String, coins: Map[String, Int]) {
    val statement = connection.prepareStatement(
      """
        |insert into PetCoins(room, nick, amount)
        |values (?, ?, ?)
      """.stripMargin)
    try {
      coins foreach {
        case (nick, amount) =>
          if (amount > 0) {
            statement.setString(1, room)
            statement.setString(2, nick)
            statement.setInt(3, amount)
            statement.executeUpdate()
          }
      }
    } finally {
      statement.close()
    }
  }

  private def deleteCoins(connection: Connection, room: String) {
    val statement = connection.prepareStatement(
      """
        |delete from PetCoins
        |where room = ?
      """.stripMargin)
    try {
      statement.setString(1, room)
      statement.executeUpdate()
    } finally {
      statement.close()
    }
  }

}
