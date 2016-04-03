package ru.org.codingteam.horta.events

trait EventEndpoint {

  /**
   * Start to gather events from this endpoint
   */
  def start(): Unit

  /**
   * Stop gathering events from this endpoint
   */
  def stop(): Unit
}
