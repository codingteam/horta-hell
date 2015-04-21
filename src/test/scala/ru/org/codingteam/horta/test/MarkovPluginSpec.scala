package ru.org.codingteam.horta.test

import akka.actor.Props
import ru.org.codingteam.horta.database.{PersistentStore, RepositoryFactory}

class MarkovPluginSpec extends TestKitSpec {

  val store = system.actorOf(Props(classOf[PersistentStore], Map[String, RepositoryFactory]()), "store")
}
