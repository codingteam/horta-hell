package ru.org.codingteam.horta.plugins

import org.joda.time.DateTime
import ru.org.codingteam.horta.security.Credential

/**
 * Trait for plugins that can process user messages.
 */
trait MessageProcessor extends BasePlugin {

  override def receive = {
    case ProcessMessage(time, credential, message) => processMessage(time, credential, message)
    case other => super.receive(other)
  }

  override def notifications = super.notifications.copy(messages = true)

  /**
   * Process a message.
   * @param time time of an event.
   * @param credential a credential of message sender.
   * @param message a message text.
   */
  protected def processMessage(time: DateTime,
                               credential: Credential,
                               message: String)

}
