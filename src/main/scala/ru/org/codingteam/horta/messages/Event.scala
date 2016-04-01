package ru.org.codingteam.horta.messages

sealed class Event()

case class TwitterEvent(tweet: String) extends Event
