package ru.org.codingteam.horta.protocol.jabber

abstract sealed class Affiliation
case object Owner extends Affiliation
case object Admin extends Affiliation
case object User extends Affiliation
case object NoneAffiliation extends Affiliation
