package ru.org.codingteam.horta.utils

class Lazy[T](ctor: => T) {
  lazy val value = ctor
}
