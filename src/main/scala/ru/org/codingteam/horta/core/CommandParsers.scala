package ru.org.codingteam.horta.core

import scala.util.parsing.combinator._
import scala.util.matching.Regex
import scala.language.postfixOps

trait CommandParsers extends RegexParsers {
  def regexMatch(r: Regex): Parser[Regex.Match] = new Parser[Regex.Match] {
    def apply(in: Input) = {
      val source = in.source
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)
      (r findPrefixMatchOf (source.subSequence(start, source.length))) match {
        case Some(matched) =>
          Success(matched, in.drop(start + matched.end - offset))
        case None =>
          Failure("string matching regex `" + r + "' expected but `" + in.first + "' found", in.drop(start - offset))
      }
    }
  }

  def replaceEscapes(xs: String, escapes: String) = {
    escapes.foldLeft(xs) {
      (acc: String, x: Char) => acc.replace("\\" + x, "" + x)
    }
  }

  def command: Parser[(String, Array[String])]
}

object SlashParsers extends CommandParsers {
  override def skipWhitespace = false

  def command_name = regexMatch("^([^\\s/]+)/".r) ^^ {
    m => m.group(1)
  }

  def argument = regexMatch("((\\\\\\\\|\\\\/|[^/])*)/".r) ^^ {
    m => replaceEscapes(m.group(1), "/\\")
  }

  override def command = command_name ~ (argument *) ^^ {
    case name ~ args => (name, args.toArray)
  }
}

object DollarParsers extends CommandParsers {
  def command_name = regexMatch("^\\$([^\\s]+)".r) ^^ {
    m => m.group(1)
  }

  def regular_argument = regexMatch("((\\\\\\\\|\\\\\\s|[^\\s])+)".r) ^^ {
    m => replaceEscapes(m.group(1), " \t\n\f\r\\")
  }

  override def command = command_name ~ (regular_argument *) ^^ {
    case name ~ args => (name, args.toArray)
  }
}
