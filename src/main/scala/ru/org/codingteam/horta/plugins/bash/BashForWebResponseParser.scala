package ru.org.codingteam.horta.plugins.bash

import org.jsoup.Jsoup
import org.apache.commons.lang3.StringEscapeUtils.{unescapeHtml4}
import util.parsing.combinator._

case class BashQuote(number: String, text: String)

object BashForWebResponseParser extends RegexParsers {
  override val skipWhitespace = false

  def apply(input: String): Option[BashQuote] = parseAll(bashForWebResponse, input) match {
    case Success(result, _) => Some(result)
    case failure: NoSuccess => println(failure.msg); None
  }

  def bashForWebResponse: Parser[BashQuote] = line ~> bashQuoteVarCat <~ rep(line) ^^ {
    case bashQuoteVarCat => {
      val div = Jsoup.parse(bashQuoteVarCat)
      val quoteId = div.getElementsByTag("a").first().text()
      val quoteText = unescapeHtml4(div.getElementById("b_q_t").html().
        replaceAll( """(<br\s*?/?>)+""", ""))

      BashQuote(quoteId, quoteText)
    }
  }

  def bashQuoteVarCat: Parser[String] = opt(spaces) ~> bashQuoteVarName ~> opt(spaces) ~> "+=" ~> opt(spaces) ~>
    bashQuoteString <~ opt(spaces) <~ semicolon <~ opt(spaces) <~ eol

  def bashQuoteString: Parser[String] = jsSimpleSingleQuotedString ~ rep(jsCat ~> jsSimpleSingleQuotedString) ^^ {
    case str ~ list => str + list.mkString("")
  }

  def jsCat: Parser[String] = opt(spaces) ~ "+" ~ opt(spaces) ^^ {
    s => ""
  }

  def spaces: Parser[String] = """[ \t]+""".r

  def jsSimpleSingleQuotedString: Parser[String] = "'" ~> "[^']*".r <~ "'"

  def bashQuoteVarName = "borq"

  def semicolon: Parser[String] = ";"

  def line: Parser[String] = """.*""".r <~ eol

  def eol: Parser[String] = "\n"
}
