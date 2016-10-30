package org.cingulata.crawler.config

import java.util.concurrent.{ExecutorService, Executors}

import com.mongodb.ServerAddress
import com.mongodb.casbah.{MongoClient, MongoCredential, MongoDB}
import com.typesafe.config.ConfigFactory
import org.cingulata.crawler.processor.{WebResourceProcessor}

/**
 * @author kuzmentsov
 * @date 16.10.16.
 */
class CrawlerConfig(resourceId: String) {
  val rootConfig = ConfigFactory.load()

  val rootSttings = rootConfig.getConfig("fetch-settings")

  val crawlerConfig = rootSttings.getConfig(resourceId)

  val processor: WebResourceProcessor = Class.forName(crawlerConfig.getString("processor")).newInstance().asInstanceOf[WebResourceProcessor]

  val crawlerHost = crawlerConfig.getString("host")
  val crawlerContextRoot = crawlerConfig.getString("contextRoot")
  val itemPageExtraParam = crawlerConfig.getString("itemPageExtraParam")

  val selectorsConfig = crawlerConfig.getConfig("selectors")
  val navigationComponentSelector = selectorsConfig.getString("navComponent")
  val linksToItemsSelector = selectorsConfig.getString("linkToItem")
  val pagingsSelector = selectorsConfig.getString("pagings")
  val pagingsDisabledSelector = selectorsConfig.getString("pagings-disabled")

  val itemSelectorsConfig = crawlerConfig.getConfig("itemSelector")
  val titleItemSelector = itemSelectorsConfig.getString("title")
  val manufacturerSelector = itemSelectorsConfig.getString("manufacturer")
  val priceItemSelector = itemSelectorsConfig.getString("price")
  val categoryItemSelector = itemSelectorsConfig.getString("category")
  val subCategoryItemSelector = itemSelectorsConfig.getString("subcategory")
  val imagesSrcSelector = itemSelectorsConfig.getString("imageSrc")

  val itemFeaturesSelectorConfig = itemSelectorsConfig.getConfig("featuresSelector")

  val featureNameSelector = itemFeaturesSelectorConfig.getString("name")
  val featureValueSelector = itemFeaturesSelectorConfig.getString("value")

  val pool: ExecutorService = Executors.newFixedThreadPool(rootConfig.getInt("threadpool.size"))

  var itemCounter = 0;

  //init of connection
  private lazy val server = new ServerAddress("ds045521.mongolab.com", 45521)

  //credentials to access database
  lazy val credentials = MongoCredential.createCredential(
    "beaver",
    "cingulata",
    "wai3Ughi".toCharArray
  )

  private val client = MongoClient(List(server), List(credentials))

  //selecting collection from properties
  val collection: MongoDB = client("cingulata")

  val itemsCollection = collection("items")

  val failedLinksCollection = collection("broken_links")
}
