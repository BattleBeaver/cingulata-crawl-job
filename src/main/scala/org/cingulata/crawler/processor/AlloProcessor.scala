package org.cingulata.crawler.processor

import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by DELL on 30.10.2016.
  */
class AlloProcessor extends WebResourceProcessor {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def parseTitle(initial: String): String = {
    try initial.substring(15)
    catch {
      case e: Exception => {
        log.error(s"Error parsing title from $initial, returning..")
        initial
      }
    }
  }

  override def parseFeatureName(initial: String): String = {
    initial.replaceAll("\\.", "").replaceAll(":", "")
  }

  override def parseManufacturer(initial: String): String = {
    initial.split("\n").foreach(line => {
      if (line.trim.startsWith("'brand'")) {
        return line.split(":")(1).replaceAll("'", "").replaceAll(",", "").trim
      }
    })
    throw new RuntimeException(s"Unknown manufacturer of $initial")
  }

}
