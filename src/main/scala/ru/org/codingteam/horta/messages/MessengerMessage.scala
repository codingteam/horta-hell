package ru.org.codingteam.horta.messages

import org.jivesoftware.smack.{Chat, XMPPConnection}
import ru.org.codingteam.horta.localization.LocaleDefinition

abstract sealed class MessengerMessage

/**
 * Reconnect request.
 * @param connection connection that was closed.
 */
case class Reconnect(connection: XMPPConnection) extends MessengerMessage

case class JoinRoom(roomJID: String,
                    locale: LocaleDefinition,
                    botName: String,
                    greeting: Option[String]) extends MessengerMessage

case class ChatOpened(chat: Chat) extends MessengerMessage
