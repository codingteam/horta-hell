package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{Credential, CommonAccess}
import scala.io.Source
import scala.util.parsing.json._

class FortunePlugin extends CommandPlugin {
	private object FortuneCommand

	private val apiCommand = "http://rexim.me/api/random"

	def commandDefinitions: List[CommandDefinition] =
		List(CommandDefinition(CommonAccess, "fortune", FortuneCommand))

	def processCommand (
		user: Credential,
		token: Any,
		arguments: Array[String]
	): Option[String] = token match {
		case FortuneCommand =>
			try {
				val rawText = Source.fromURL(apiCommand).mkString
				val json = JSON.parseFull(rawText)
				val map = json.get.asInstanceOf[Map[String, Any]]
				val body = map.get("body").map(_.asInstanceOf[String])
				val id = map.get("id").map(_.asInstanceOf[Double])
				(id, body) match {
					case (Some(id), Some(body)) => Some(s"#${id.toInt}\n$body")
					case _ => Some("[ERROR] Wrong response from the service.")
				}
			} catch {
				case e: Exception => {
					e.printStackTrace()
					Some("[ERROR] Something's wrong!")
				}
			}

		case _ => None
	}
}
