package ru.org.codingteam.horta.plugins.markov

import me.fornever.platonus.Network
import ru.org.codingteam.horta.plugins.log.{LogRepository, MessageType}

object LogParser {

  def parse(log: LogRepository, roomName: String, userName: String): Network = {
    val messages = log.getMessagesByUser(roomName, userName, MessageType).toStream
    val phrases = messages.map(message => tokenize(message.text))

    phrases.foldLeft(Network(2)){ (network, message) =>
      network.add(message)
    }
  }

  def tokenize(message: String) = {
    message.split("\\s+").toVector
  }

}