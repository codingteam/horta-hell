package ru.org.codingteam.horta.plugins.pet

object DyingPet {

  def unapply(pet: PetData) = pet.health <= 0 || pet.satiation <= 0

}

object HungerPet {

  def unapply(pet: PetData) = pet.satiation <= Pet.HUNGER_BOUNDS._2 && pet.satiation > Pet.HUNGER_BOUNDS._1

}

object IllPet {

  def unapply(pet: PetData) = pet.health <= Pet.HEALTH_BOUNDS._2 && pet.health > Pet.HEALTH_BOUNDS._1

}