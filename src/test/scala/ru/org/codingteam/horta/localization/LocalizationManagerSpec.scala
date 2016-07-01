package ru.org.codingteam.horta.localization

import org.scalatest.{FlatSpec, Matchers}

class LocalizationManagerSpec extends FlatSpec with Matchers {

  "A FileLocalizationManager" should "list ru and en locales" in {
    val manager = new FileLocalizationManager("./src/main/resources/localization")
    val locales = manager.locales

    assert(List("ru", "en").forall(name => locales.contains(LocaleDefinition(name))))
  }

  "A FileLocalizationManager" should "should create a reader for en locale" in {
    val manager = new FileLocalizationManager("./src/main/resources/localization")
    val reader = manager.getReader("en")
    assert(reader !== null)
  }

  "A ResourceLocalizationManager" should "list ru and en locales" in {
    val locales = ResourceLocalizationManager.locales

    assert(List("ru", "en").forall(name => locales.contains(LocaleDefinition(name))))
  }

  "A ResourceLocalizationManager" should "should create a reader for en locale" in {
    val reader = ResourceLocalizationManager.getReader("en")
    assert(reader !== null)
  }

}
