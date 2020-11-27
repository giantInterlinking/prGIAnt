package progressiveAlgorithms;

import datamodel.Tile;
import datamodel.GeometryIndex;
import datamodel.LightIndex;
import datamodel.Pair;
import datamodel.PairIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import utilities.CsvReader;
import utilities.DecPairWeightComparator;
import utilities.DecTileCardinalityComparator;
import utilities.IncTileCardinalityComparator;
import utilities.WeightingScheme;

public class ProgressiveRADON extends AbstractProgressiveAlgorithm {

    protected final boolean decreasingOrder;

    protected Tile[] blocksArray;
    protected List<Tile> blocksList;
    protected GeometryIndex geometryIndex;
    protected final Geometry[] targetData;

    public ProgressiveRADON(boolean decOrder, int budget, int qPairs, String delimiter,
            String sourceFilePath, String targetFilePath, WeightingScheme wScheme)
            throws IOException {
        super(budget, qPairs, delimiter, sourceFilePath, targetFilePath, wScheme);

        decreasingOrder = decOrder;
        targetData = CsvReader.readAllEntities(delimiter, targetFilePath);
    }

    private List<Tile> getBlocks(LightIndex spatialIndex) {
        final List<Tile> blocks = new ArrayList<>();
        final TIntObjectMap<TIntObjectMap<TIntList>> map = spatialIndex.getMap();
        final TIntObjectIterator<TIntObjectMap<TIntList>> outerIterator = map.iterator();
        while (outerIterator.hasNext()) {
            outerIterator.advance();

            final int xDimension = outerIterator.key();
            final TIntObjectMap<TIntList> value = outerIterator.value();
            final TIntObjectIterator<TIntList> innerIterator = value.iterator();
            while (innerIterator.hasNext()) {
                innerIterator.advance();

                if (innerIterator.value().size() < 2) {
                    continue;
                }

                final int yDimension = innerIterator.key();
                final TIntList entitiesD1 = new TIntArrayList();
                final TIntList entitiesD2 = new TIntArrayList();
                final TIntIterator entityIterator = innerIterator.value().iterator();
                while (entityIterator.hasNext()) {
                    int currentId = entityIterator.next();
                    if (currentId < datasetDelimiter) {
                        entitiesD1.add(currentId);
                    } else {
                        entitiesD2.add(currentId - datasetDelimiter);
                    }
                }

                // each block should contain entities from both datasets
                if (entitiesD1.isEmpty() || entitiesD2.isEmpty()) {
                    continue;
                }

                blocks.add(new Tile(xDimension, yDimension, entitiesD1.toArray(), entitiesD2.toArray()));
            }
        }

        return blocks;
    }

    @Override
    public String getMethodName() {
        return "Progressive RADON";
    }

    protected float getNoOfBlocks() {
        return geometryIndex.getNoOfBlocks();
    }

    protected float getNoOfCommonBlocks(int sourceId, int targetId) {
        return geometryIndex.getNoOfCommonBlocks(sourceId, targetId);
    }

    protected float getNoOfGeometryBlocks(int geometryId) {
        return geometryIndex.getNoOfEntityBlocks(geometryId);
    }

    protected float getWeight(int sourceId, int targetId) {
        float commonBlocks = getNoOfCommonBlocks(sourceId, targetId);
        switch (wScheme) {
            case CF:
                return commonBlocks;
            case JS:
                return commonBlocks / (getNoOfGeometryBlocks(sourceId) + getNoOfGeometryBlocks(targetId) - commonBlocks);
            case X2:
                long[] v = new long[2];
                v[0] = (long) commonBlocks;
                v[1] = (long) getNoOfGeometryBlocks(sourceId) - v[0];

                long[] v_ = new long[2];
                v_[0] = (long) getNoOfGeometryBlocks(targetId) - v[0];
                v_[1] = (long) (getNoOfBlocks() - (v[0] + v[1] + v_[0]));

                return (float) chiSquaredTest.chiSquare(new long[][]{v, v_});
        }

        return 1.0f;
    }

    @Override
    protected void filtering() {
        setThetas();
        indexSource();
        indexTarget();
    }

    private void indexTarget() {
        int geometryId = datasetDelimiter;
        for (Geometry tEntity : targetData) {
            final Envelope envelope = tEntity.getEnvelopeInternal();
            addToIndex(geometryId, envelope, spatialIndex);
            geometryId++;
        }
    }

    @Override
    public void scheduling() {
        blocksList = getBlocks(spatialIndex);
        if (decreasingOrder) {
            Collections.sort(blocksList, new DecTileCardinalityComparator());
        } else {
            Collections.sort(blocksList, new IncTileCardinalityComparator());
        }
        blocksArray = blocksList.toArray(new Tile[blocksList.size()]);
        geometryIndex = new GeometryIndex(datasetDelimiter, sourceData.length + targetData.length, blocksArray);
    }
    
    @Override
    protected void setThetas() {
        float thetaXa = 0;
        float thetaYa = 0;
        for (Geometry sEntity : sourceData) {
            final Envelope en = sEntity.getEnvelopeInternal();
            thetaXa += en.getWidth();
            thetaYa += en.getHeight();
        }
        thetaXa /= sourceData.length;
        thetaYa /= sourceData.length;

        float thetaXb = 0;
        float thetaYb = 0;
        for (Geometry tEntity : targetData) {
            final Envelope en = tEntity.getEnvelopeInternal();
            thetaXb += en.getWidth();
            thetaYb += en.getHeight();
        }
        thetaXb /= targetData.length;
        thetaYb /= targetData.length;
        thetaX = 0.5f * (thetaXa + thetaXb);
        thetaY = 0.5f * (thetaYa + thetaYb);
        System.out.println(thetaX + "\t" + thetaY);
    }

    @Override
    protected void verification() {
        int verifiedPairs = -1;
        final List<Pair> topPairs = new ArrayList<>();
        for (Tile block : blocksList) {
            topPairs.clear();
            final PairIterator iterator = block.getPairIterator();
            while (iterator.hasNext()) {
                final Pair pair = iterator.next();
                if (validCandidate(pair.getEntityId1(), targetData[pair.getEntityId2()].getEnvelopeInternal(), block)) {
                    float weight = getWeight(pair.getEntityId1(), pair.getEntityId2() + datasetDelimiter);
                    pair.setWeight(weight);
                    topPairs.add(pair);
                }
            }
            Collections.sort(topPairs, new DecPairWeightComparator());

            for (Pair p : topPairs) {
                verifiedPairs++;
                if (verifiedPairs == budget) {
                    return;
                }
                relations.verifyRelations(p.getEntityId1(), p.getEntityId2(), sourceData[p.getEntityId1()], targetData[p.getEntityId2()]);
                if (relations.getVerifiedPairs() % 631064 == 0) {
                    System.out.println(relations.getVerifiedPairs() + "\t" + relations.getInterlinkedPairs());
                }
            }
        }
    }

    @Override
    protected boolean validCandidate(int candidateId, Envelope e2, Tile bTile) {
        int xDimension = (int) Math.max(sourceData[candidateId].getEnvelopeInternal().getMinX() / thetaX, e2.getMinX() / thetaX);
        int yDimension = (int) Math.min(sourceData[candidateId].getEnvelopeInternal().getMaxY() / thetaY, e2.getMaxY() / thetaY);
        if (xDimension == bTile.getXDimension() && yDimension == bTile.getYDimension()) {
            return sourceData[candidateId].getEnvelopeInternal().intersects(e2);
        }
        return false;
    }

    @Override
    protected float getNoOfCommonBlocks(int sourceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
