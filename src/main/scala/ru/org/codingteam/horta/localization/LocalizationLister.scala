package ru.org.codingteam.horta.localization

import java.io.Reader

case class LocaleDefinition(name: String)

trait LocalizationLister {

  final val nameRegex = "^(.*)\\.conf$".r

  def locales: Map[LocaleDefinition, LocalizationMap]
  def getReader(localeName: String): Reader

}
