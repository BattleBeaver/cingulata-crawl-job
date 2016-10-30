package org.cingulata.crawler.util

import java.net.SocketTimeoutException

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

/**
 * Created by kuzmentsov@gmail.com on 17.10.15.
 */
object CrawlerKit {

  private val DUMMY_DOC = new Document("DUMMY_DOC")
  private val priceRegEx: Regex ="[^\\d]".r
  private val log: Logger = LoggerFactory.getLogger("CrawlerKit")

  /**
   * Getting content of document by it's URL, adding UserAgent parameter.
   * @param url
   * @param retryAfter
   * @return
   */
  @throws(classOf[SocketTimeoutException])
  @tailrec def documentOf(url: String, retryAfter: Int = Int.MinValue): Document = {
    if (retryAfter != Int.MinValue) {
      Thread.sleep(retryAfter)
    }
    val response = Jsoup.connect(url).timeout(Int.MaxValue).userAgent("cingulata.crawler-system:1.0").ignoreHttpErrors(true).execute()
    response.statusCode() match {
      case 200 => response.parse()
      case 404 => {
        log.error(s"Got 404 at $url")
        new Document("DUMMY_DOC")
      }
      case 429 => {
        val retryAfter = response.header("Retry-After").toInt
        log.error(s"Got 429, Retry-After set to $retryAfter, url: $url")
        if (retryAfter == Int.MinValue) {
          log.error("Retried already - no luck.")
          DUMMY_DOC
        } else {
          log.error("Retrying ....")
          documentOf(url, retryAfter)
        }
      }
    }
  }

  /**
   * Curried function to parse several patterns from one source.
   * @param url
   * @return
   */
  def findOn(url: String): String => Elements = {
    (selector: String) => documentOf(url).select(selector)
  }

  /**
   * Get price from string-represented, unformatted value.
   * @param f
   * @param pattern
   * @return
   */
  def matchPrice(f: (String)  => Elements, pattern: String): Double = {
    Option(f(pattern).first) match {
      case Some(element) => priceFromString(element.text)
      case None => -0.1d
    }
  }

  /**
   * Get price from string-represented, unformatted value.
   * @param priceString
   * @return
   */
  def priceFromString(priceString: String): Double = {
    try {
      priceRegEx.replaceAllIn(priceString, "").toDouble
    } catch {
      case nfe: NumberFormatException => -0.1d
    }
  }
}
