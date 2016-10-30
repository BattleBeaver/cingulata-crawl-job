package org.cingulata.crawler

import java.net.SocketTimeoutException

import com.mongodb.casbah._
import com.mongodb.casbah.commons.MongoDBObject
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.cingulata.crawler.config.CrawlerConfig
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.collection.immutable.Map
import org.cingulata.crawler.util.CrawlerKit._
import org.cingulata.crawler.model.{Item, Link}

import scala.collection.mutable.ArrayBuffer
import scala.collection.convert.wrapAsScala._

object CrawlerApp {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val config: CrawlerConfig = new CrawlerConfig("allo.ua")
  /**
   * Thread's Overriden Run method.
   */
  def main(args: Array[String]): Unit = {
    log.info(s"Crawler started to parse ${config.crawlerContextRoot}")

    log.info(s"Host: ${config.crawlerHost}")
    log.info(s"ContextRoot: ${config.crawlerContextRoot}")
    parseNavigation(config.crawlerContextRoot)

    log.info(s"Crawler finished to parse ${config.crawlerContextRoot}")
  }

  /**
   * Parse links to navigation elements.
   * @param url
   */
  private def parseNavigation(url: String): Unit = {
    def searchFor = findOn(url)
    val navigationLinks = new java.util.ArrayList[Element](searchFor(config.navigationComponentSelector)).map(x => directURL(x.attr("href"))).toList

    //navigationLinks = navigationLinks.slice(0, 2) //TODO: Temp
    println(s"Navigation: \n\t${navigationLinks.toArray.mkString("\n\t")}")


    val conf = new SparkConf().setAppName("Cingulata Web Crawler")
    val sc = new SparkContext(conf)

    val navigationLinksRdd: RDD[String] = sc.parallelize(navigationLinks)

    val linksToItems = navigationLinksRdd.map(link => {
      println(link)
      getLinksToItemsInfo(link)
    })
      .flatMap(l => l)
      .foreach(l => saveToDB(parseItemFromUrl(l)))

    println(linksToItems)
  }

  @tailrec private def getLinksToItemsInfo(linkToItems: String, accum: ArrayBuffer[String] = ArrayBuffer.empty): Seq[String] = {
    def searchFor = findOn(linkToItems)

    val linksToItems = searchFor(config.linksToItemsSelector).map(_.attr("href"))

    //adding current process to accumulator
    accum ++= linksToItems

    val pagings: Elements = searchFor(config.pagingsSelector)


    if (!searchFor(config.pagingsDisabledSelector).isEmpty) {
      println(s">>>> paging finished at ${linkToItems}")
      return accum
    }

    if(pagings.isEmpty) {
      println(s">>>>NO pagings found at ${linkToItems}")
      return accum
    }

    val link = directURL(pagings.get(0).child(0).attr("href"))
    println(s">>>> paging found : $link on ${linkToItems}, buffer size: ${accum.size}")

    getLinksToItemsInfo(link, accum)
  }

  /**
   * Process Links to Items.
   * @param linksToItems
   */
  def process(linksToItems: Elements): Unit = {
    //val builder = itemsCollection.initializeUnorderedBulkOperation
    for(i <- 0 until linksToItems.size) {
      log.info(linksToItems.get(i).attr("href"))
      saveToDB(parseItemFromUrl(directURL(linksToItems.get(i).attr("href"))))
    }
  }

  def process(link: String): Unit = {
    saveToDB(parseItemFromUrl(directURL(link)))
  }


  /**
   * Parses Item from given URL.
   * @param url
   * @return
   */
    def parseItemFromUrl(url: String): Item = {
    val directUrl = directItemURL(url)
    try {
      def searchFor = findOn(directUrl)
      val title: String = config.processor.parseTitle(searchFor(config.titleItemSelector).text)
      val manufacturer: String = config.processor.parseManufacturer(searchFor(config.manufacturerSelector).html)
      val price: Double = matchPrice(searchFor, config.priceItemSelector)
      val category: String = searchFor(config.categoryItemSelector).text
      val subcategory: String = searchFor(config.subCategoryItemSelector).text

      val imageSrc: String = searchFor(config.imagesSrcSelector).attr("src")
      config.itemCounter += 1;
      log.info(s"${config.itemCounter} $title $directUrl")
      val features: Map[String, String] = parseItemFeatures(searchFor)
      return Item(url, title, manufacturer, category, subcategory, price, features)
    } catch {
      case e: SocketTimeoutException => {
        log.error(s"Exception occured while processing parseItemFromUrl($url)", e)
        saveBrokenLink(Link(url, "Item Parse Error"))
        return null
      }
    }
  }

  /**
   * Saves Link which was parsed with an Exception.
   * @param link
   */
  def saveBrokenLink(link: Link): Unit = {
    config.failedLinksCollection += MongoDBObject(
      "url" -> link.url,
      "title" -> link.title
    )
  }

  /**
   * Saves item to DB.
   * @param item
   */
  def saveToDB(item: Item): Unit = {
    try {
      config.itemsCollection += MongoDBObject(
        "host" -> config.crawlerHost,
        "url" -> item.url,
        "title" -> item.title,
        "manufacturer" -> item.manufacturer,
        "category" -> item.category,
        "subcategory" -> item.subcategory,
        "price" -> item.price,
        "features" -> Implicits.map2MongoDBObject(item.features)
      )
    } catch {
      case e: Exception => log.info("Error While Saving to collection\n", e); return
    }
  }

  /**
   * Items features parsing
   * @param parsingFunction
   * @return
   */
  private def parseItemFeatures(parsingFunction: (String) => Elements): Map[String, String] = {
    val names = parsingFunction(config.featureNameSelector)
    val values = parsingFunction(config.featureValueSelector)

    val features = scala.collection.mutable.Map[String, String]()
    for(i <- 0 until names.size) {
      features(config.processor.parseFeatureName(names.get(i).text)) = values.get(i).text
    }
    return features.toMap
  }

  /**
   * If URL starts with http: - it is formed in proper way.
   * In other case prepends contextroot of site to URL.
   * @param url
   * @return
   */
  def directURL(url: String) = if(url.startsWith("http")) url  else config.crawlerContextRoot + url

  /**
   * If URL starts with http: - it is formed in proper way.
   * In other case prepends contextroot of site to URL.
   * @param url
   * @return
   */
  def directItemURL(url: String) = if(url.startsWith("http")) url + config.itemPageExtraParam else config.crawlerContextRoot + url + config.itemPageExtraParam

}