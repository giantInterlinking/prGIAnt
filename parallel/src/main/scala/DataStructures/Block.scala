package DataStructures

import utils.Constants.Relation.Relation
import utils.Utils



case class Block(id: Long, coords: (Int, Int), source: Array[SpatialEntity], target: Array[SpatialEntity]) {


	/**
	 * Return all blocks comparisons
	 * @return block's comparisons
	 */
	def getComparisons: Array[(SpatialEntity, SpatialEntity)]= for (s <-source; t <- target) yield (s, t)

	/**
	 *
	 * @return the total comparisons of the block
	 */
	def getTotalComparisons: Int = source.length * target.length

	/**
	 * Return only the comparisons that their MBBs relate and that their\
	 * reference points are inside the block
	 *
	 * @return blocks comparisons after filtering
	 */
	def getFilteredComparisons(relation: Relation): Array[(SpatialEntity, SpatialEntity)] =
		for (s <-source; t <- target; if s.testMBB(t, relation) && s.referencePointFiltering(t, coords, Utils.thetaXY))
			yield (s, t)

	def getSourceIDs: Array[String] = source.map(se => se.originalID)

	def getTargetIDs: Array[String] = target.map(se => se.originalID)

	def getSourceSize: Long = source.length

	def getTargetSize: Long = target.length

}

object Block {
	def apply(coords: (Int, Int), source: Array[SpatialEntity], target: Array[SpatialEntity]): Block ={
		Block(Utils.signedPairing(coords._1, coords._2), coords, source, target)
	}
}