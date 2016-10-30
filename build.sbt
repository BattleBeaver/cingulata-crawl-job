name := "cingulata-crawl-job"

version := "1.0"

scalaVersion := "2.11.8"

lazy val jsoupVersion = "1.8.3"
lazy val casbahVersion = "3.0.0"

//MongoDB
libraryDependencies += "org.mongodb" %% "casbah" % casbahVersion
//JSoup
libraryDependencies += "org.jsoup" % "jsoup" % jsoupVersion

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.0.1" % "provided"