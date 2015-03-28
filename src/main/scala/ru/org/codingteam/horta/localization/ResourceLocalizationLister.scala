package ru.org.codingteam.horta.localization

import java.io.{InputStreamReader, Reader, File}
import java.nio.file.Paths
import java.util.jar.{JarEntry, JarFile}
import scala.collection.JavaConversions._

class ResourceLocalizationLister extends LocalizationLister {

  val resourcePath = "localization"
  
  val fsPath = getClass.getProtectionDomain.getCodeSource.getLocation.toURI
  val location = Paths.get(fsPath).toFile
  val isJarFile = location.isFile // It may be a JAR file when running a program from JAR or a file system location otherwise
  lazy val fileLister = new FileLocalizationLister(Paths.get(location.getAbsolutePath, resourcePath).toString)

  override def locales: Map[LocaleDefinition, LocalizationMap] = {
    if (isJarFile) {
      jarLocales
    } else {
      fileLister.locales
    }
  }

  override def getReader(localeName: String): Reader = {
    if (isJarFile) {
      getJarReader(localeName)
    } else {
      fileLister.getReader(localeName)
    }
  }

  private def jarLocales = {
    val jar = new JarFile(location)
    jar.entries.toStream.filter(_.getName.startsWith(resourcePath + "/")).flatMap(entry => {
      val mbName = getLocaleName(entry)
      mbName map { name =>
        (LocaleDefinition(name), new LocalizationMap(this, name))
      }
    }).toMap
  }

  private def getLocaleName(entry: JarEntry) = {
    val fileName = entry.getName.split("/").last
    fileName match {
      case nameRegex(name) => Some(name)
      case _ => None
    }
  }

  private def getJarReader(localeName: String) =
    new InputStreamReader(ClassLoader.getSystemResourceAsStream(s"$resourcePath/$localeName.conf"), "UTF8")

}
