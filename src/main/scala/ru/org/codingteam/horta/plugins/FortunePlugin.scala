package ru.org.codingteam.horta.plugins

import ru.org.codingteam.horta.security.{CommandContext, Scope, GlobalScope}
import scala.util.parsing.json._
import scala.io.Source

class FortunePlugin extends CommandPlugin {
	private object FortuneCommand

	private val maxCharacters = 100
	private val fortuneUrl =
		s"http://www.iheartquotes.com/api/v1/random?max_characters=${maxCharacters}&format=json"

	def commandDefinitions: List[CommandDefinition] =
		List(CommandDefinition(GlobalScope, "fortune", FortuneCommand))

	def processCommand (
		token: Any,
		scope: Scope,
		context: CommandContext,
		arguments: Array[String]
	): Option[String] = token match {
		case FortuneCommand =>
			try {
				val rawText = Source.fromURL(fortuneUrl).mkString
				val json = JSON.parseFull(rawText)
				val map = json.get.asInstanceOf[Map[String, Any]]
				map.get("quote").map(_.asInstanceOf[String])
			} catch {
				case e: Exception => {
					e.printStackTrace()
					Some("Что-то не так.")
				}
			}

		case _ => None
	}
}
