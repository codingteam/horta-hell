package ru.org.codingteam.horta.protocol.jabber

abstract class Affinity
case object Owner extends Affinity
case object Admin extends Affinity
case object User extends Affinity
