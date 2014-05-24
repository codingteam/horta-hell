package ru.org.codingteam.horta.plugins.mail

private case class MailMessage(id: Option[Int], room: String, senderNick: String, receiverNick: String, text: String)
