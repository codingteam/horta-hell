package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.messages.SendResponse

/**
 * Test plugin. Its work is to respond "test" to any test request.
 */
class TestPlugin extends CommandPlugin {

  private object TestCommand

  /**
   * A collection of (token -> scope) pairs, where token defines command token and scope - its scope.
   * @return collection.
   */
  def commandDefinitions: List[CommandDefinition] = List(CommandDefinition(CommonAccess, "test", TestCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case TestCommand => credential.location ! SendResponse(credential, "test")
      case _ =>
    }
  }

}
