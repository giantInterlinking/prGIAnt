package DataStructures

import com.vividsolutions.jts.geom.{Coordinate, Envelope, Geometry, GeometryFactory}
import utils.Constants.Relation
import utils.Constants.Relation.Relation
import math._


/**
 * Minimum Bounding Box
 *
 * @param maxX max x
 * @param minX min x
 * @param maxY max y
 * @param minY min y
 */
case class MBB(maxX:Double, minX:Double, maxY:Double, minY:Double){


    /**
     * return true if the reference point is in the block.
     * The reference point is the upper left point of their intersection
     *
     * @param mbb the mbb that intersects
     * @param b the examined block
     * @return true if the reference point is in the block
     */
    private[DataStructures]
    def referencePointFiltering(mbb:MBB, b:(Int, Int), thetaXY: (Double, Double)): Boolean ={
        val (thetaX, thetaY) = thetaXY

        val minX1 = minX / thetaX
        val minX2 = mbb.minX / thetaX
        val maxY1 = maxY / thetaY
        val maxY2 = mbb.maxY / thetaY

        val rf: (Double, Double) =(max(minX1, minX2), min(maxY1, maxY2))
        rf._1 < b._1 && rf._1+1 >= b._1 && rf._2 < b._2 && rf._2+1 >= b._2
    }

    private[DataStructures]
    def referencePointFiltering(mbb:MBB, b:(Int, Int), thetaXY: (Double, Double), partition: MBB): Boolean ={
        val (thetaX, thetaY) = thetaXY

        val minX1 = minX / thetaX
        val minX2 = mbb.minX / thetaX
        val maxY1 = maxY / thetaY
        val maxY2 = mbb.maxY / thetaY

        val rf: (Double, Double) =(max(minX1, minX2)+.0000001, min(maxY1, maxY2)+.0000001)
        val rfMBB = MBB(rf._1, rf._2)
        val blockMBB = MBB(b._1+1, b._1, b._2+1, b._2)
        blockMBB.contains(rfMBB) && partition.contains(rfMBB)
    }

    def partitionRF(mbb:MBB, thetaXY: (Double, Double), partition: MBB): Boolean ={
        val (thetaX, thetaY) = thetaXY

        val minX1 = minX / thetaX
        val minX2 = mbb.minX / thetaX
        val maxY1 = maxY / thetaY
        val maxY2 = mbb.maxY / thetaY

        val rf: (Double, Double) =(max(minX1, minX2)+.0000001, min(maxY1, maxY2)+.0000001)
        val rfMBB = MBB(rf._1, rf._2)
        partition.contains(rfMBB)
    }

    /**
     *  check relation among MBBs
     *
     * @param mbb MBB to examine
     * @param relations requested relations
     * @return whether the relation is true
     */
    private[DataStructures]
    def testMBB(mbb:MBB, relations: Seq[Relation]): Boolean ={
        relations.map {
            case Relation.CONTAINS | Relation.COVERS =>
                contains(mbb)
            case Relation.WITHIN | Relation.COVEREDBY =>
                within(mbb)
            case Relation.INTERSECTS | Relation.CROSSES | Relation.OVERLAPS =>
                intersects(mbb)
            case Relation.TOUCHES => touches(mbb)
            case Relation.DISJOINT => disjoint(mbb)
            case Relation.EQUALS => equals(mbb)
            case _ => false
        }.reduce( _ || _)

    }

    /**
     * convert MBB into jts.Geometry
     * @return jts.Geometry
     */
    def getGeometry: Geometry = {
        val gf: GeometryFactory = new GeometryFactory()
        if (minX == maxX)
            gf.createPoint(new Coordinate(minX, minY))
        else {
            val coordsList: List[(Double, Double)] = List((minX, minY), (minX, maxY), (maxX, maxY), (maxX, minY), (minX, minY))
            val coordsAr: Array[Coordinate] = coordsList.map(c => new Coordinate(c._1, c._2)).toArray
            gf.createPolygon(coordsAr)
        }
    }


    override def toString: String =
        "(" + minX.toString  + ", " + maxX.toString +"), ("+ minY.toString  + ", " + maxY.toString +")"


    /**
     * check if the mbb is equal to the given one
     * @param mbb given mbb
     * @return whether it's true
     */
    private[DataStructures]
    def equals(mbb:MBB): Boolean ={
        minX == mbb.minX && maxX == mbb.maxX && minY == mbb.minY && maxY == mbb.maxY
    }

    /**
     * check if the mbb contains the given one
     * @param mbb given mbb
     * @return whether it's true
     */
    private[DataStructures]
    def contains(mbb:MBB): Boolean ={
        minX <= mbb.minX && maxX >= mbb.maxX && minY <= mbb.minY && maxY >= mbb.maxY
    }

    /**
     * check if the mbb is within to the given one
     * @param mbb given mbb
     * @return whether it's true
     */
    private[DataStructures]
    def within(mbb: MBB):Boolean ={
        mbb.contains(this)
    }

    /**
     * check if the mbb touches the given one
     * @param mbb given mbb
     * @return whether it's true
     */
    private[DataStructures]
    def touches(mbb: MBB): Boolean ={
        maxX == mbb.maxX || minX == mbb.minX || maxY == mbb.maxY || minY == mbb.minY
    }

    /**
     * check if the mbb intersects the given one
     * @param mbb given mbb
     * @return whether it's true
     */
    private[DataStructures]
    def intersects(mbb:MBB): Boolean ={
        ! disjoint(mbb)
    }

    /**
     * check if the mbb disjoints the given one
     * @param mbb given mbb
     * @return whether it's true
     */
    private[DataStructures]
    def disjoint(mbb:MBB): Boolean ={
        minX > mbb.maxX || maxX < mbb.minX || minY > mbb.maxY || maxY < mbb.minY
    }


    def adjust(thetaXY: (Double, Double)) : MBB ={
        val (thetaX, thetaY) = thetaXY

        val maxX = this.maxX / thetaX
        val minX = this.minX / thetaX
        val maxY = this.maxY / thetaY
        val minY = this.minY / thetaY

        MBB(maxX, minX, maxY, minY)
    }
}

object  MBB {
    def apply(geom: Geometry): MBB ={
        val env = geom.getEnvelopeInternal
        MBB(env.getMaxX, env.getMinX, env.getMaxY, env.getMinY)
    }

    def apply(env: Envelope): MBB ={
        MBB(env.getMaxX, env.getMinX, env.getMaxY, env.getMinY)
    }

    def apply(x:Double, y:Double): MBB ={
       MBB(x, x, y, y)
    }
}
