package EntityMatching.DistributedMatching

import DataStructures.SpatialEntity
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import utils.Configuration
import utils.Constants.WeightStrategy.WeightStrategy
import utils.Constants.{MatchingAlgorithm, WeightStrategy}

object DMFactory {

    val log: Logger = LogManager.getRootLogger

    def getMatchingAlgorithm(conf: Configuration, source: RDD[(Int, SpatialEntity)], target: RDD[(Int, SpatialEntity)],
                             partitioner: Partitioner, budgetArg: Int = -1, wsArg: String = "", ma: String = ""): DMTrait ={

        val algorithm = if(MatchingAlgorithm.exists(ma)) MatchingAlgorithm.withName(ma) else conf.getMatchingAlgorithm
        val budget = if(budgetArg > 0) budgetArg else conf.getBudget
        val ws: WeightStrategy = if(WeightStrategy.exists(wsArg.toString)) WeightStrategy.withName(wsArg) else conf.getWeightingScheme

        algorithm match {
            case MatchingAlgorithm.PROGRESSIVE_GIANT =>
                log.info("Matching Algorithm: " + MatchingAlgorithm.PROGRESSIVE_GIANT)
                ProgressiveGIAnt(source, target, ws, budget, partitioner)
            case _ =>
                log.info("Matching Algorithm: " + MatchingAlgorithm.GIANT)
                GIAnt(source, target, partitioner)
        }
    }

}
