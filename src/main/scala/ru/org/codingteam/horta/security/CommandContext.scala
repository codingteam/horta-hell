package ru.org.codingteam.horta.security

import akka.actor.ActorRef

/**
 * Command context. Contains definition of command sender.
 * @param contextHolder reference to actor that contain sender definition.
 */
case class CommandContext (val contextHolder: ActorRef)
