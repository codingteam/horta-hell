package ru.org.codingteam.horta.localization

import java.nio.file.Paths

import com.typesafe.config.{ConfigUtil, ConfigFactory, ConfigValue, ConfigValueType}
import ru.org.codingteam.horta.configuration.Configuration

import scala.collection.JavaConversions._
import scala.util.Random

class LocalizationMap(localeName: String) {

  private lazy val (values, arrays) = parseLocalization()

  def get(key: String) = values.get(key)
  def random(key: String) = arrays.get(key) map { case vector =>
    val index = Random.nextInt(vector.size)
    vector.get(index)
  }

  private def parseLocalization(): (Map[String, String], Map[String, Vector[String]]) = {
    val path = Paths.get(Configuration.localizationPath, s"$localeName.conf")
    val file = path.toFile
    val conf = ConfigFactory.parseFile(file)
    val (valueEntries, listEntries) = conf.entrySet.partition { case entry =>
      entry.getValue.valueType == ConfigValueType.STRING
    }

    val values = processStrings(valueEntries.toStream)
    val arrays = processLists(listEntries.toStream)

    (values, arrays)
  }

  private def getKey(entry: java.util.Map.Entry[String, ConfigValue]) = ConfigUtil.splitPath(entry.getKey).last

  private def processStrings(entries: Stream[java.util.Map.Entry[String, ConfigValue]]): Map[String, String] = {
    entries.map(entry => (
      getKey(entry),
      entry.getValue.unwrapped.asInstanceOf[String]
    )).toMap
  }

  private def processLists(entries: Stream[java.util.Map.Entry[String, ConfigValue]]): Map[String, Vector[String]] = {
    entries.map { case entry =>
      val key = getKey(entry)
      val unwrapped = entry.getValue.unwrapped.asInstanceOf[java.util.List[Object]]
      (key, unwrapped.toStream.map(_.asInstanceOf[String]).toVector)
    }.toMap
  }

}
