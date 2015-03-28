package ru.org.codingteam.horta.localization

import ru.org.codingteam.horta.security.Credential

object Localization {

  private val locales: Map[LocaleDefinition, LocalizationMap] = loadLocales()

  def localize(key: String)(implicit credential: Credential): String = localize(key, credential.locale)
  def random(key: String)(implicit credential: Credential): String = random(key, credential.locale)

  def localize(key: String, locale: LocaleDefinition): String = {
    withLocale(locale) { case map =>
      map.get(key)
    } getOrElse key
  }

  def random(key: String, locale: LocaleDefinition): String = {
    withLocale(locale) { case map =>
      map.random(key)
    } getOrElse key
  }

  private def loadLocales() = ResourceLocalizationManager.locales

  private def withLocale(locale: LocaleDefinition)
                        (action: LocalizationMap => Option[String]): Option[String] = {
    locales.get(locale).flatMap(action)
  }

}
