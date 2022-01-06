package progressiveAlgorithms;

import com.google.common.collect.MinMaxPriorityQueue;
import datamodel.Pair;
import datamodel.Tile;
import utilities.WeightingScheme;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import utilities.CsvReader;
import utilities.IncPairWeightComparator;

public class ProgressiveGIAnt extends AbstractProgressiveAlgorithm {

    protected int[] flag;
    protected int[] frequency;

    protected double minimumWeight;

    protected MinMaxPriorityQueue topKPairs;

    public ProgressiveGIAnt(int budget, int qPairs, String delimiter,
            String sourceFilePath, String targetFilePath, WeightingScheme wScheme)
            throws IOException {
        super(budget, qPairs, delimiter, sourceFilePath, targetFilePath, wScheme);

        minimumWeight = 0;
        flag = new int[datasetDelimiter];
        frequency = new int[datasetDelimiter];
        topKPairs = MinMaxPriorityQueue.orderedBy(new IncPairWeightComparator()).maximumSize(2 * budget).create();
    }

    protected TIntSet getCandidates(int referenceId, Geometry tEntity) {
        final TIntSet candidateMatches = new TIntHashSet();

        final Envelope envelope = tEntity.getEnvelopeInternal();
        int maxX = (int) Math.ceil(envelope.getMaxX() / thetaX);
        int maxY = (int) Math.ceil(envelope.getMaxY() / thetaY);
        int minX = (int) Math.floor(envelope.getMinX() / thetaX);
        int minY = (int) Math.floor(envelope.getMinY() / thetaY);

        for (int latIndex = minX; latIndex <= maxX; latIndex++) {
            for (int longIndex = minY; longIndex <= maxY; longIndex++) {
                final TIntList partialCandidates = spatialIndex.getSquare(latIndex, longIndex);
                if (partialCandidates != null) {
                    final TIntIterator iterator = partialCandidates.iterator();
                    while (iterator.hasNext()) {
                        int currentId = iterator.next();
                        if (flag[currentId] != referenceId) {
                            flag[currentId] = referenceId;
                            frequency[currentId] = 0;
                        }
                        frequency[currentId]++;
                        candidateMatches.add(currentId);
                    }
                }
            }
        }

        return candidateMatches;
    }

    @Override
    public String getMethodName() {
        return "Progressive GIA.nt";
    }

    @Override
    protected float getNoOfCommonBlocks(int sourceId) {
        return frequency[sourceId];
    }

    @Override
    protected void scheduling() {
        int geoCollections = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(targetFilePath));
            String line = reader.readLine();
            int counter = 0;

            while (line != null) {
                String[] tokens = line.split(delimiter);
                try {
                    if (2 <= tokens.length) {
                        Geometry tEntity = CsvReader.WKT_READER.read(tokens[1].trim());
                        if (tEntity == null) {
                            line = reader.readLine();
                            continue;
                        }

                        if (tEntity.getGeometryType().equals("GeometryCollection")) {
                            geoCollections++;
                            line = reader.readLine();
                            continue;
                        }
                        
                        final TIntSet candidateMatches = getCandidates(counter, tEntity);

                        final TIntIterator intIterator = candidateMatches.iterator();
                        while (intIterator.hasNext()) {
                            int candidateMatchId = intIterator.next();
                            if (validCandidate(candidateMatchId, tEntity.getEnvelopeInternal(), null)) {
                                float weight = getWeight(candidateMatchId, tEntity, wScheme);
                                if (minimumWeight <= weight) {
                                    final Pair p = new Pair(candidateMatchId, counter, weight, tEntity);
                                    topKPairs.add(p);
                                    if (budget < topKPairs.size()) {
                                        Pair lastPair = (Pair) topKPairs.poll();
                                        minimumWeight = lastPair.getWeight();
                                    }
                                }
                            }
                        }

                        counter++;
                    }
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }

                line = reader.readLine();
            }
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Target geometry collections\t:\t" + geoCollections);
    }

    @Override
    protected void setThetas() {
        thetaX = 0;
        thetaY = 0;
        for (Geometry sEntity : sourceData) {
            final Envelope en = sEntity.getEnvelopeInternal();
            thetaX += en.getWidth();
            thetaY += en.getHeight();
        }
        thetaX /= sourceData.length;
        thetaY /= sourceData.length;
        System.out.println(thetaX + "\t" + thetaY);
    }

    @Override
    protected boolean validCandidate(int candidateId, Envelope e2, Tile bTile) {
        return sourceData[candidateId].getEnvelopeInternal().intersects(e2);
    }

    @Override
    protected void verification() {
        while (!topKPairs.isEmpty()) {
            final Pair p = (Pair) topKPairs.pollLast();
            relations.verifyRelations(p.getEntityId1(), p.getEntityId2(), sourceData[p.getEntityId1()], p.getTargetGeometry());
        }
    }
}
