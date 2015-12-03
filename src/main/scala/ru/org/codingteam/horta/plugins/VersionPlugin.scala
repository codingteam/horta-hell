package ru.org.codingteam.horta.plugins

import java.util.Properties

import ru.org.codingteam.horta.core.Core
import ru.org.codingteam.horta.protocol.Protocol
import ru.org.codingteam.horta.security.{CommonAccess, Credential}

private object VersionCommand

class VersionPlugin extends BasePlugin with CommandProcessor {

  override def name = "version"

  override def commands = List(CommandDefinition(CommonAccess, "version", VersionCommand))

  override def processCommand(credential: Credential,
                              token: Any,
                              arguments: Array[String]) = {
    token match {
      case VersionCommand =>
        val impl = (for (p <- Option(classOf[Core].getPackage);
                         v <- Option(p.getImplementationVersion)) yield v) getOrElse "development"
        val version = s"version: ${VersionPlugin.version} build-id: ${VersionPlugin.buildId} impl-version: $impl"
        Protocol.sendResponse(credential.location, credential, version)
      case _ =>
    }
  }
}

object VersionPlugin {

  lazy val (version, buildId) = {
    val p = new Properties
    val in = getClass.getResourceAsStream("/ru/org/codingteam/horta/version.properties")
    try
      p.load(in)
    finally
      in.close()
    (p get "version", p get "buildId")
  }
}
