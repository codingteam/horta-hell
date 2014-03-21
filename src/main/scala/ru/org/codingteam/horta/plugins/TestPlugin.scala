package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import ru.org.codingteam.horta.messages.{SendPrivateResponse, SendResponse}

private object TestCommand

/**
 * Test plugin. Its work is to respond "test" to any test request.
 */
class TestPlugin extends CommandPlugin {
  def pluginDefinition = PluginDefinition(false, List(CommandDefinition(CommonAccess, "test", TestCommand)))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) {
    token match {
      case TestCommand =>
        val location = credential.location
        location ! SendResponse(credential, "test")
        location ! SendPrivateResponse(credential, "test")
      case _ =>
    }
  }

}
