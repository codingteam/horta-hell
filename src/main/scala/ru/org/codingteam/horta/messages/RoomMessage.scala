package ru.org.codingteam.horta.messages

import akka.actor.ActorRef

abstract class RoomMessage
case class InitializeRoom(roomName: String, messenger: ActorRef) extends RoomMessage
case class UserMessage(jid: String, message: String) extends RoomMessage
case class ParsedPhrase(nick: String, message: String) extends RoomMessage
case class GeneratedPhrase(forNick: String, phrase: String) extends RoomMessage

