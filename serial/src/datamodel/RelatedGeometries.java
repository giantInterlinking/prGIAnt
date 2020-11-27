package datamodel;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.IntersectionMatrix;

public class RelatedGeometries {

    private int exceptions;
    private int detectedLinks;
    private int interlinkedGeometries;
    public final int qualifyingPairs;
    private int verifiedPairs;

    private float pgr;

    private final TIntList containsD1;
    private final TIntList containsD2;
    private final TIntList coveredByD1;
    private final TIntList coveredByD2;
    private final TIntList coversD1;
    private final TIntList coversD2;
    private final TIntList crossesD1;
    private final TIntList crossesD2;
    private final TIntList equalsD1;
    private final TIntList equalsD2;
    private final TIntList intersectsD1;
    private final TIntList intersectsD2;
    private final TIntList overlapsD1;
    private final TIntList overlapsD2;
    private final TIntList touchesD1;
    private final TIntList touchesD2;
    private final TIntList withinD1;
    private final TIntList withinD2;

    public RelatedGeometries(int qualifyingPairs) {
        pgr = 0;
        exceptions = 0;
        detectedLinks = 0;
        verifiedPairs = 0;
        this.qualifyingPairs = qualifyingPairs;
        interlinkedGeometries = 0;

        containsD1 = new TIntArrayList();
        containsD2 = new TIntArrayList();
        coveredByD1 = new TIntArrayList();
        coveredByD2 = new TIntArrayList();
        coversD1 = new TIntArrayList();
        coversD2 = new TIntArrayList();
        crossesD1 = new TIntArrayList();
        crossesD2 = new TIntArrayList();
        equalsD1 = new TIntArrayList();
        equalsD2 = new TIntArrayList();
        intersectsD1 = new TIntArrayList();
        intersectsD2 = new TIntArrayList();
        overlapsD1 = new TIntArrayList();
        overlapsD2 = new TIntArrayList();
        touchesD1 = new TIntArrayList();
        touchesD2 = new TIntArrayList();
        withinD1 = new TIntArrayList();
        withinD2 = new TIntArrayList();
    }

    private void addContains(int gId1, int gId2) {
        containsD1.add(gId1);
        containsD2.add(gId2);
    }

    private void addCoveredBy(int gId1, int gId2) {
        coveredByD1.add(gId1);
        coveredByD2.add(gId2);
    }

    private void addCovers(int gId1, int gId2) {
        coversD1.add(gId1);
        coversD2.add(gId2);
    }

    private void addCrosses(int gId1, int gId2) {
        crossesD1.add(gId1);
        crossesD2.add(gId2);
    }

    private void addEquals(int gId1, int gId2) {
        equalsD1.add(gId1);
        equalsD2.add(gId2);
    }

    private void addIntersects(int gId1, int gId2) {
        intersectsD1.add(gId1);
        intersectsD2.add(gId2);
    }

    private void addOverlaps(int gId1, int gId2) {
        overlapsD1.add(gId1);
        overlapsD2.add(gId2);
    }

    private void addTouches(int gId1, int gId2) {
        touchesD1.add(gId1);
        touchesD2.add(gId2);
    }

    private void addWithin(int gId1, int gId2) {
        withinD1.add(gId1);
        withinD2.add(gId2);
    }

    public int getInterlinkedPairs() {
        return interlinkedGeometries;
    }

    private int getNoOfContains() {
        return containsD1.size();
    }

    private int getNoOfCoveredBy() {
        return coveredByD1.size();
    }

    private int getNoOfCovers() {
        return coversD1.size();
    }

    private int getNoOfCrosses() {
        return crossesD1.size();
    }

    private int getNoOfEquals() {
        return equalsD1.size();
    }

    private int getNoOfIntersects() {
        return intersectsD1.size();
    }

    private int getNoOfOverlaps() {
        return overlapsD1.size();
    }

    private int getNoOfTouches() {
        return touchesD1.size();
    }

    private int getNoOfWithin() {
        return withinD1.size();
    }

    public int getVerifiedPairs() {
        return verifiedPairs;
    }

    public void print() {
        System.out.println("Qualifying pairs\t:\t" + qualifyingPairs);
        System.out.println("Exceptions\t:\t" + exceptions);
        System.out.println("Detected Links\t:\t" + detectedLinks);
        System.out.println("Interlinked geometries\t:\t" + interlinkedGeometries);
        System.out.println("No of contains\t:\t" + getNoOfContains());
        System.out.println("No of covered-by:\t" + getNoOfCoveredBy());
        System.out.println("No of covers\t:\t" + getNoOfCovers());
        System.out.println("No of crosses\t:\t" + getNoOfCrosses());
        System.out.println("No of equals\t:\t" + getNoOfEquals());
        System.out.println("No of intersects:\t" + getNoOfIntersects());
        System.out.println("No of overlaps\t:\t" + getNoOfOverlaps());
        System.out.println("No of touches\t:\t" + getNoOfTouches());
        System.out.println("No of within\t:\t" + getNoOfWithin());
        System.out.println("Recall\t:\t" + (interlinkedGeometries / (double) qualifyingPairs));
        System.out.println("Precision\t:\t" + (interlinkedGeometries / (double) verifiedPairs));
        System.out.println("Progressive Geometry Recall\t:\t" + pgr / qualifyingPairs / verifiedPairs);
        System.out.println("Verified pairs\t:\t" + verifiedPairs);
    }

    public int isVerification(int geomId1, int geomId2, Geometry sourceGeom, Geometry targetGeom) {
        if (!sourceGeom.getEnvelopeInternal().intersects(targetGeom.getEnvelopeInternal())) {
            return -1;
        }

        final int dimension1 = sourceGeom.getDimension();
        final int dimension2 = targetGeom.getDimension();
        final IntersectionMatrix im = sourceGeom.relate(targetGeom);

        if (im.isContains()) {
            return 1;
        }
        if (im.isCoveredBy()) {
            return 1;
        }
        if (im.isCovers()) {
            return 1;
        }
        if (im.isCrosses(dimension1, dimension2)) {
            return 1;
        }
        if (im.isEquals(dimension1, dimension2)) {
            return 1;
        }
        if (im.isIntersects()) {
            return 1;
        }
        if (im.isOverlaps(dimension1, dimension2)) {
            return 1;
        }
        if (im.isTouches(dimension1, dimension2)) {
            return 1;
        }
        if (im.isWithin()) {
            return 1;
        }

        return 0;
    }

    public boolean verifyRelations(int geomId1, int geomId2, Geometry sourceGeom, Geometry targetGeom) {
        try {
            final int dimension1 = sourceGeom.getDimension();
            final int dimension2 = targetGeom.getDimension();
            final IntersectionMatrix im = sourceGeom.relate(targetGeom);

            verifiedPairs++;
            boolean related = false;
            if (im.isContains()) {
                related = true;
                detectedLinks++;
                addContains(geomId1, geomId2);
            }
            if (im.isCoveredBy()) {
                related = true;
                detectedLinks++;
                addCoveredBy(geomId1, geomId2);
            }
            if (im.isCovers()) {
                related = true;
                detectedLinks++;
                addCovers(geomId1, geomId2);
            }
            if (im.isCrosses(dimension1, dimension2)) {
                related = true;
                detectedLinks++;
                addCrosses(geomId1, geomId2);
            }
            if (im.isEquals(dimension1, dimension2)) {
                related = true;
                detectedLinks++;
                addEquals(geomId1, geomId2);
            }
            if (im.isIntersects()) {
                related = true;
                detectedLinks++;
                addIntersects(geomId1, geomId2);
            }
            if (im.isOverlaps(dimension1, dimension2)) {
                related = true;
                detectedLinks++;
                addOverlaps(geomId1, geomId2);
            }
            if (im.isTouches(dimension1, dimension2)) {
                related = true;
                detectedLinks++;
                addTouches(geomId1, geomId2);
            }
            if (im.isWithin()) {
                related = true;
                detectedLinks++;
                addWithin(geomId1, geomId2);
            }

            if (related) {
                interlinkedGeometries++;
            }
            pgr += interlinkedGeometries;

//            if (verifiedPairs == 5000000 || verifiedPairs == 10000000) {
//                System.out.println("\nTime\t:\t" + System.currentTimeMillis());
//                System.out.println(pgr + "\t" + interlinkedGeometries + "\t" + verifiedPairs);
//                System.out.println(pgr / qualifyingPairs / verifiedPairs);
//            }
            return related;
        } catch (Exception ex) {
            ex.printStackTrace();
            exceptions++;
            return false;
        }
    }
}
