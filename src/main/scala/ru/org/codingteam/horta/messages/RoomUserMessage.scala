package ru.org.codingteam.horta.messages

import akka.actor.ActorRef
import platonus.Network

abstract class RoomUserMessage
case class AddPhrase(phrase: String) extends RoomUserMessage
case class GeneratePhrase(forNick: String) extends RoomUserMessage
case class CalculateDiff(forNick: String, nick1: String, nick2: String, roomUser2: ActorRef) extends RoomUserMessage
case class CalculateDiffRequest(room: ActorRef, forNick: String, nick1: String, nick2: String, network1: Network)
  extends RoomUserMessage
