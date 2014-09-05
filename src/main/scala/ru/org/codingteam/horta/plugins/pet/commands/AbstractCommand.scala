package ru.org.codingteam.horta.plugins.pet.commands

import akka.actor.ActorRef
import ru.org.codingteam.horta.plugins.pet.PetData
import ru.org.codingteam.horta.security.Credential

trait AbstractCommand extends ((PetData, ActorRef, Credential, Array[String]) => (PetData, String)) {

}
