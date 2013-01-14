package ru.org.codingteam.horta.security

import akka.actor.ActorRef

class User(val jid: String, val role: UserRole, val location: ActorRef) {

}

object User {
  def fromJid(jid: String, location: ActorRef) = {
    // TODO: add known users
    new User(jid, UnknownUser, location)
  }
}