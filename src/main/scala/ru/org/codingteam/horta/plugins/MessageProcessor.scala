package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.Credential

/**
 * Trait for plugins that can process user messages.
 */
trait MessageProcessor extends BasePlugin {

  override def receive = {
    case ProcessMessage(credential, message) => processMessage(credential, message)
    case other => super.receive(other)
  }

  override def notifications = super.notifications.copy(messages = true)

  /**
   * Process a message.
   * @param credential a credential of message sender.
   * @param message a message text.
   */
  protected def processMessage(credential: Credential,
                               message: String) = ()

}
