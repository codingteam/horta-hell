package ru.org.codingteam.horta.plugins.bash

import org.jsoup.Jsoup
import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4
import util.parsing.combinator._

case class BashQuote(number: String, rate: String, text: String)

object BashForWebResponseParser extends RegexParsers {
  override val skipWhitespace = false

  def apply(input: String): Option[BashQuote] = {
    try {
      parseAll(bashForWebResponse, input) match {
        case Success(result, _) => Some(result)
        case failure: NoSuccess =>
          println(failure.msg)
          None
      }
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }

  def bashForWebResponse: Parser[BashQuote] = line ~> bashQuoteVarCat <~ rep(line) ^^ {
    case bashQuoteVarCat =>
      val bashQuoteContentElementId = "b_q_t"
      val bashQuoteRateElementId = "b_q_h"

      val bashQuoteDiv = Jsoup.parse(bashQuoteVarCat)
      val quoteId = bashQuoteDiv.getElementsByTag("a").first().text()
      val quoteRate = bashQuoteDiv.getElementById(bashQuoteRateElementId).text()
      val bashQuoteContentDiv = bashQuoteDiv.getElementById(bashQuoteContentElementId)

      bashQuoteContentDiv.getElementsByTag("br").append("\\n")

      val quoteText = unescapeHtml4(bashQuoteContentDiv.text().replaceAll("""\\n""", "\n"))

      BashQuote(quoteId, quoteRate, quoteText)
  }

  def bashQuoteVarCat: Parser[String] = opt(spaces) ~> bashQuoteVarName ~> opt(spaces) ~> "+=" ~> opt(spaces) ~>
    bashQuoteString <~ opt(spaces) <~ semicolon <~ opt(spaces) <~ eol

  def bashQuoteString: Parser[String] = jsSimpleSingleQuotedString ~ rep(jsConcat ~> jsSimpleSingleQuotedString) ^^ {
    case str ~ list => str + list.mkString("")
  }

  def jsConcat = opt(spaces) ~ '+' ~ opt(spaces)

  def spaces = """[ \t]+""".r

  def jsSimpleSingleQuotedString: Parser[String] = "'" ~> "[^']*".r <~ "'"

  def bashQuoteVarName = "borq"

  def semicolon = ';'

  def line = ".*".r <~ eol

  def eol = '\n'
}
