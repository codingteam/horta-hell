package ru.org.codingteam.horta.localization

import ru.org.codingteam.horta.utils.Lazy

case class LocaleDefinition(name: String)

object Localization {

  private val locales: Map[LocaleDefinition, LocalizationMap] = loadLocales()

  def get(implicit locale: LocaleDefinition)(key: String) = {

  }

  def random()

  def loadLocales() = ???

}
