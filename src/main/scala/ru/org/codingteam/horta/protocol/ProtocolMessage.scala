package ru.org.codingteam.horta.protocol

import ru.org.codingteam.horta.security.Credential
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.Future

abstract private sealed class ProtocolMessage()

private case class SendMucMessage(toJID: String, message: String) extends ProtocolMessage()

private case class SendPrivateMessage(roomJID: String, nick: String, message: String) extends ProtocolMessage()

private case class SendChatMessage(toJID: String, message: String) extends ProtocolMessage()

/**
 * Request to send response to user.
 * @param credential user credential.
 * @param text response text.
 */
private case class SendResponse(credential: Credential, text: String) extends ProtocolMessage()

/**
 * Request to send response to user through the private message.
 * @param credential user credential.
 * @param text response text.
 */
private case class SendPrivateResponse(credential: Credential, text: String) extends ProtocolMessage()