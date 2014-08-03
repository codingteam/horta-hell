package ru.org.codingteam.horta.plugins.pet

import java.sql.{Connection, Timestamp}
import org.joda.time.DateTime

import ru.org.codingteam.horta.database.DAO

class PetDAO extends DAO {

  override def schema = "pet"

  override def store(connection: Connection, id: Option[Any], obj: Any): Option[Any] = {
    id match {
      case Some(room) =>
        val roomName = room.asInstanceOf[String]
        val select = connection.prepareStatement("SELECT * FROM pet WHERE room = ?")
        try {
          select.setString(1, roomName)
          val resultSet = select.executeQuery()
          try {
            if (resultSet.next()) {
              update(connection, roomName, obj)
            } else {
              insert(connection, roomName, obj)
            }

            Some(null)
          } finally {
            resultSet.close()
          }
        } finally {
          select.close()
        }

      case None => throw new IllegalArgumentException("id should not be None")
    }
  }

  override def read(connection: Connection, id: Any): Option[Any] = {
    val roomName = id.asInstanceOf[String]
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
              resultSet.getInt("hunger"),
              new DateTime(resultSet.getTimestamp("birth")),
              readCoins(connection, roomName)))
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

  override def delete(connection: Connection, id: Any): Boolean = false

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

  private def insert(connection: Connection, room: String, obj: Any) {
    val PetData(nickname, alive, health, hunger, birth, coins) = obj
    val statement = connection.prepareStatement(
      "INSERT INTO pet (room, nickname, alive, health, hunger, birth) VALUES (?, ?, ?, ?, ?, ?)")
    try {
      statement.setString(1, room)
      statement.setString(2, nickname)
      statement.setBoolean(3, alive)
      statement.setInt(4, health)
      statement.setInt(5, hunger)
      statement.setTimestamp(6, new Timestamp(birth.getMillis))
      statement.executeUpdate()

      insertCoins(connection, room, coins)
    } finally {
      statement.close()
    }
  }

  private def update(connection: Connection, room: String, obj: Any) {
    val PetData(nickname, alive, health, hunger, birth, coins) = obj
    val statement = connection.prepareStatement(
      "UPDATE pet SET nickname = ?, alive = ?, health = ?, hunger = ?, birth = ? WHERE room = ?")
    try {
      statement.setString(1, nickname)
      statement.setBoolean(2, alive)
      statement.setInt(3, health)
      statement.setInt(4, hunger)
      statement.setTimestamp(5, new Timestamp(birth.getMillis))
      statement.setString(6, room)
      statement.executeUpdate()

      deleteCoins(connection, room)
      insertCoins(connection, room, coins)
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
          statement.setString(1, room)
          statement.setString(2, nick)
          statement.setInt(3, amount)
          statement.executeUpdate()
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
