package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.Pet
import ru.org.codingteam.horta.security.Credential

trait AbstractCommand extends ((Pet, Credential, Array[String]) => (Pet, String)) {

}
