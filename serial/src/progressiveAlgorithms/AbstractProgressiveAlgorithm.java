package progressiveAlgorithms;

import datamodel.Tile;
import datamodel.LightIndex;
import datamodel.RelatedGeometries;
import java.io.IOException;
import utilities.WeightingScheme;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import utilities.CsvReader;

public abstract class AbstractProgressiveAlgorithm {

    protected long indexingTime;
    protected long initializationTime;
    protected long verificationTime;

    protected float thetaX;
    protected float thetaY;

    protected int budget;
    protected int datasetDelimiter;
    protected int noOfApproxBlocks;

    protected ChiSquareTest chiSquaredTest;
    protected final Geometry[] sourceData;
    protected LightIndex spatialIndex;
    protected final RelatedGeometries relations;
    protected final String delimiter;
    protected final String targetFilePath;
    protected final WeightingScheme wScheme;
    protected static final WKTReader WKT_READER = new WKTReader();

    public AbstractProgressiveAlgorithm(int budget, int qPairs, String delimiter, String sourceFilePath,
            String targetFilePath, WeightingScheme wScheme) throws IOException {
        this.budget = budget;
        this.wScheme = wScheme;
        this.delimiter = delimiter;

        sourceData = CsvReader.readAllEntities(delimiter, sourceFilePath);
        this.targetFilePath = targetFilePath;

        datasetDelimiter = sourceData.length;
        relations = new RelatedGeometries(qPairs);
        if (wScheme.equals(WeightingScheme.X2)) {
            chiSquaredTest = new ChiSquareTest();
        }
    }

    protected void addToIndex(int geometryId, Envelope envelope, LightIndex spatialIndex) {
        int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
        int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
        int minX = (int) Math.floor(envelope.getMinX() / thetaX);
        int minY = (int) Math.floor(envelope.getMinY() / thetaY);

        for (int latIndex = minX; latIndex <= maxX; latIndex++) {
            for (int longIndex = minY; longIndex <= maxY; longIndex++) {
                spatialIndex.add(latIndex, longIndex, geometryId);
            }
        }
    }

    public void applyProcessing() {
        long time1 = System.currentTimeMillis();
        filtering();
        long time2 = System.currentTimeMillis();
        scheduling();
        long time3 = System.currentTimeMillis();
        verification();
        long time4 = System.currentTimeMillis();
        indexingTime = time2 - time1;
        initializationTime = time3 - time2;
        verificationTime = time4 - time3;
    }

    protected void filtering() {
        setThetas();
        indexSource();
    }

    public abstract String getMethodName();

    protected long getNoOfBlocks(Envelope envelope) {
        int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
        int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
        int minX = (int) Math.floor(envelope.getMinX() / thetaX);
        int minY = (int) Math.floor(envelope.getMinY() / thetaY);
        return (maxX - minX + 1) * (maxY - minY + 1);
    }

    protected abstract float getNoOfCommonBlocks(int sourceId);

    protected float getWeight(int sourceId, Geometry tEntity, WeightingScheme wScheme) {
        float commonBlocks = getNoOfCommonBlocks(sourceId);
        switch (wScheme) {
            case CF:
                return commonBlocks;
            case JS:
                final Envelope sourceEnv = sourceData[sourceId].getEnvelopeInternal();
                final Envelope targetEnv = tEntity.getEnvelopeInternal();
                return commonBlocks / (getNoOfBlocks(sourceEnv) + getNoOfBlocks(targetEnv) - commonBlocks);
            case X2:
                long[] va  = new long[2];
                va[0] = (long) commonBlocks;
                va[1] = getNoOfBlocks(tEntity.getEnvelopeInternal()) - va[0];

                long[] va_ = new long[2];
                va_[0] = getNoOfBlocks(sourceData[sourceId].getEnvelopeInternal()) - va[0];
                va_[1] = (int) Math.max(1, noOfApproxBlocks - (va[0] + va[1] + va_[0]));

                return (float) chiSquaredTest.chiSquare(new long[][]{va, va_});
            case MBR:
                final Envelope srcEnv = sourceData[sourceId].getEnvelopeInternal();
                final Envelope trgEnv = tEntity.getEnvelopeInternal();
                final Envelope mbrIntersection = srcEnv.intersection(trgEnv);
                float denominator = ((float) srcEnv.getArea()) + ((float) trgEnv.getArea()) - ((float) mbrIntersection.getArea());
                if (denominator == 0) {
                    return 0;
                }
                return (float) (mbrIntersection.getArea() / denominator);
            case POINTS:
                return (float) 1.0 / (sourceData[sourceId].getNumPoints() + tEntity.getNumPoints());
        }

        return 1.0f;
    }

    protected LightIndex indexSource() {
        spatialIndex = new LightIndex();

        int geometryId = 0;
        for (Geometry sEntity : sourceData) {
            final Envelope envelope = sEntity.getEnvelopeInternal();
            addToIndex(geometryId, envelope, spatialIndex);
            geometryId++;
        }

        if (wScheme.equals(WeightingScheme.X2)) {
            setApproximateNoOfBlocks();
        }

        return spatialIndex;
    }

    public void printResults() {
        System.out.println("\n\nCurrent method\t:\t" + getMethodName());
        System.out.println("Indexing Time\t:\t" + indexingTime);
        System.out.println("Initialization Time\t:\t" + initializationTime);
        System.out.println("Verification Time\t:\t" + verificationTime);
        relations.print();
    }

    protected abstract void scheduling();
    
    protected void setApproximateNoOfBlocks() {
        int globalMaxX = 0;
        int globalMaxY = 0;
        int globalMinX = 0;
        int globalMinY = 0;
        for (Geometry sEntity : sourceData) {
            final Envelope envelope = sEntity.getEnvelopeInternal();
            int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
            int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
            int minX = (int) Math.floor(envelope.getMinX() / thetaX);
            int minY = (int) Math.floor(envelope.getMinY() / thetaY);

            if (globalMaxX < maxX) {
                globalMaxX = maxX;
            }

            if (globalMaxY < maxY) {
                globalMaxY = maxY;
            }

            if (minX < globalMinX) {
                globalMinX = minX;
            }

            if (minY < globalMinY) {
                globalMinY = minY;
            }
        }

        noOfApproxBlocks = (globalMaxX - globalMinX + 1) * (globalMaxY - globalMinY + 1);
    }

    protected abstract void setThetas();

    protected abstract boolean validCandidate(int candidateId, Envelope e2, Tile bTile);

    protected abstract void verification();
}
