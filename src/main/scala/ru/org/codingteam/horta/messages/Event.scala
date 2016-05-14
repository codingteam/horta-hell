package ru.org.codingteam.horta.messages

sealed class Event()

case class TwitterEvent(author: String, tweet: String) extends Event
