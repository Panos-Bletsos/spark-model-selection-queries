package me.panosbletsos

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.IOUtils
import org.apache.spark.sql.SparkSession

object ModelSelectionQueries extends App {
  override def main(args: Array[String]): Unit = {
    val conf: Config = ConfigFactory.load

    val spark: SparkSession = SparkSession
      .builder()
      .appName("model-selection-stats")
      .config("spark.sql.warehouse.dir", conf.getString("warehouseLocation"))
      .enableHiveSupport()
      .getOrCreate()

    val dbNames = conf.getString("databases").replace(" ", "").split(",")
    dbNames.foreach(
      runQueries(_, spark, conf.getInt("iterations"), conf.getLong("autoBroadcastJoinThreshold")))
  }

  def getQueries: Seq[String] = {
    val queryNames = Seq("q1", "q2", "q3", "q4", "q5", "q6", "q7")
    queryNames.map(queryName => IOUtils.toString(
      getClass.getClassLoader.getResourceAsStream(s"queries/$queryName.sql")))
  }

  def runQueries(
    db: String, spark: SparkSession, iterations: Int, joinThreshold: Long): Unit = {
    val queries = getQueries
    spark.sql(s"use $db")
    1 to iterations foreach { _ =>
      spark.sqlContext.setConf("spark.sql.autoBroadcastJoinThreshold", "-1")
      queries.foreach(spark.sql(_).count)
    }
    1 to iterations foreach { _ =>
      spark.sqlContext.setConf("spark.sql.autoBroadcastJoinThreshold", joinThreshold.toString)
      queries.foreach(spark.sql(_).count)
    }
  }
}
