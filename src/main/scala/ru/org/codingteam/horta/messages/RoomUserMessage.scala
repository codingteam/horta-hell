package ru.org.codingteam.horta.messages

import ru.org.codingteam.horta.security.Credential
import me.fornever.platonus.Network

abstract sealed class RoomUserMessage

case class UserPhrase(phrase: String) extends RoomUserMessage

case class AddPhrase(phrase: String) extends RoomUserMessage

case class GeneratePhrase(credential: Credential, length: Integer, allCaps: Boolean) extends RoomUserMessage

case class ReplaceRequest(credential: Credential, from: String, to: String) extends RoomUserMessage
