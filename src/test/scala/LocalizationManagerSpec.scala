import org.scalatest.{Matchers, FlatSpec}
import ru.org.codingteam.horta.localization.{ResourceLocalizationManager, LocaleDefinition, FileLocalizationManager}

class LocalizationManagerSpec extends FlatSpec with Matchers {

  "A FileLocalizationManager" should "list ru and en locales" in {
    val lister = new FileLocalizationManager("./src/main/resources/localization")
    val locales = lister.locales

    assert(List("ru", "en").forall(name => locales.contains(LocaleDefinition(name))))
  }

  "A FileLocalizationManager" should "should create a reader for en locale" in {
    val lister = new FileLocalizationManager("./src/main/resources/localization")
    val reader = lister.getReader("en")
    assert(reader !== null)
  }

  "A ResourceLocalizationManager" should "list ru and en locales" in {
    val lister = new ResourceLocalizationManager()
    val locales = lister.locales

    assert(List("ru", "en").forall(name => locales.contains(LocaleDefinition(name))))
  }

  "A ResourceLocalizationManager" should "should create a reader for en locale" in {
    val lister = new ResourceLocalizationManager()
    val reader = lister.getReader("en")
    assert(reader !== null)
  }

}
