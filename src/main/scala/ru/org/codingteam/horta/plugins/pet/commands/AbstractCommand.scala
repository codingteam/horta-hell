package ru.org.codingteam.horta.plugins.pet.commands

import ru.org.codingteam.horta.plugins.pet.PetData
import ru.org.codingteam.horta.security.Credential

trait AbstractCommand extends ((PetData, Credential, Array[String]) => (PetData, String)) {

}
