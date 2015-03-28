import org.scalatest.{Matchers, FlatSpec}
import ru.org.codingteam.horta.localization.{ResourceLocalizationLister, LocaleDefinition, FileLocalizationLister}

class LocalizationListerSpec extends FlatSpec with Matchers {

  "A FileLocalizationLister" should "list ru and en locales" in {
    val lister = new FileLocalizationLister("./src/main/resources/localization")
    val locales = lister.locales

    assert(List("ru", "en").forall(name => locales.contains(LocaleDefinition(name))))
  }

  "A FileLocalizationLister" should "should create a reader for en locale" in {
    val lister = new FileLocalizationLister("./src/main/resources/localization")
    val reader = lister.getReader("en")
    assert(reader !== null)
  }

  "A ResourceLocalizationLister" should "list ru and en locales" in {
    val lister = new ResourceLocalizationLister()
    val locales = lister.locales

    assert(List("ru", "en").forall(name => locales.contains(LocaleDefinition(name))))
  }

  "A ResourceLocalizationLister" should "should create a reader for en locale" in {
    val lister = new ResourceLocalizationLister()
    val reader = lister.getReader("en")
    assert(reader !== null)
  }

}
