package ru.org.codingteam.horta.actors.messenger

/**
 * Message class for the Messenger.
 */
abstract sealed class MessengerMessage

/**
 * Room topic increment request.
 * @param room target room.
 */
case class IncrementTopic(room: String)
