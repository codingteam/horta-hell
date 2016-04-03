package ru.org.codingteam.horta.events

trait EndpointFactory {

  def construct (eventCollector: EventCollector): EventEndpoint
}
