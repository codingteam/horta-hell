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

  /**
   * Gather events here
   */
  def process(eventCollector: EventCollector): Unit

  /**
   * Perform the necessary checks to see if endpoint is configured correctly
   * @return true if configuration is valid, false otherwise
   */
  def validate():Boolean
}
