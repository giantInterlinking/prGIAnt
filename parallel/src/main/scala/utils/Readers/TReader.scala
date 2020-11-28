package utils.Readers

import DataStructures.SpatialEntity
import org.apache.spark.rdd.RDD

/**
 */
trait TReader {

  def load( filePath: String,
                    realID_field: String,
                    geometryField: String
                  ): RDD[SpatialEntity]
}
