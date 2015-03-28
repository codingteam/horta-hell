package ru.org.codingteam.horta.localization

import java.io.{FileInputStream, InputStreamReader, FileReader, Reader}
import java.nio.file.{Files, Paths}

import scala.collection.JavaConversions._

class FileLocalizationLister(path: String) extends LocalizationLister {

  override def locales: Map[LocaleDefinition, LocalizationMap] = {
    Files.newDirectoryStream(Paths.get(path)).toStream.map { case filePath =>
      val fileName = filePath.getFileName.toString
      val localeName = fileName match {
        case nameRegex(name) => name
        case _ => sys.error(s"Invalid path entry $fileName")
      }

      (LocaleDefinition(localeName), new LocalizationMap(this, localeName))
    }.toMap
  }

  override def getReader(localeName: String): Reader =
    new InputStreamReader(new FileInputStream(Paths.get(path, localeName) + ".conf"), "UTF8")

}
