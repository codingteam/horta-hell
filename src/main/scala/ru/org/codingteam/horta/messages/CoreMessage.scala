package ru.org.codingteam.horta.messages

import ru.org.codingteam.horta.security.Credential

/**
 * Core system message.
 * @param credential user credential.
 * @param text message text.
 */
case class CoreMessage(credential: Credential,
                       text: String)

/**
 * Request to send response to user.
 * @param credential user credential.
 * @param text response text.
 */
case class SendResponse(credential: Credential,
                        text: String)