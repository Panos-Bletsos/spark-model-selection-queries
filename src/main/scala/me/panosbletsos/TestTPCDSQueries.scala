package me.panosbletsos

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.IOUtils
import org.apache.spark.sql.SparkSession

object TestTPCDSQueries extends App {
  override def main(args: Array[String]): Unit = {
    val conf: Config = ConfigFactory.load

    val spark: SparkSession = SparkSession
      .builder()
      .appName("test-tpcds-queries")
      .config("spark.sql.warehouse.dir", conf.getString("warehouseLocation"))
      .enableHiveSupport()
      .getOrCreate()

    val dbNames = conf.getString("databases").replace(" ", "").split(",")
    dbNames.foreach(runQueries(
      _, conf.getString("TestTPCDSQueries.queries"), spark, conf.getInt("iterations")))
  }

  def getQueries(queries: String): Seq[String] = {
    val queryNames: Seq[String] = queries.replace(" ", "").split(",")
    queryNames.map(queryName => IOUtils.toString(
      getClass.getClassLoader.getResourceAsStream(s"test-tpcds-queries/$queryName.sql")))
  }

  def runQueries(db: String, queryNames: String, spark: SparkSession, iterations: Int): Unit = {
    val queries = getQueries(queryNames).zipWithIndex
    spark.sql(s"use $db")
    1 to iterations foreach(i => queries.foreach { q =>
      spark.sparkContext.queryName = s"query${q._2}$db"
      spark.sql(q._1).count
    })
  }
}
