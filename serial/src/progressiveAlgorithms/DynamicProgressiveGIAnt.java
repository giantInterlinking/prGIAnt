package progressiveAlgorithms;

import datamodel.SortPair;
import datamodel.Tile;
import utilities.WeightingScheme;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import utilities.CsvReader;

/**
 *
 * @author gap2
 */
public class DynamicProgressiveGIAnt extends AbstractProgressiveAlgorithm {

    private final boolean localCandidates;

    protected int[] flag;
    protected int[] frequency;

    protected double minimumWeight;

    protected TreeSet<SortPair> topKPairs;
    protected final TIntObjectMap<List<SortPair>> sourceCandidates;
    protected final TIntObjectMap<List<SortPair>> targetCandidates;

    public DynamicProgressiveGIAnt(int budget, int qPairs, String delimiter,
            String sourceFilePath, String targetFilePath, WeightingScheme wScheme)
            throws IOException {
        super(budget, qPairs, delimiter, sourceFilePath, targetFilePath, wScheme);

        minimumWeight = 0;
        topKPairs = new TreeSet<>();
        sourceCandidates = new TIntObjectHashMap<>();
        targetCandidates = new TIntObjectHashMap<>();

        if (wScheme == WeightingScheme.MBR && wScheme == WeightingScheme.POINTS) {
            localCandidates = true;
        } else {
            localCandidates = false;
            flag = new int[datasetDelimiter];
            frequency = new int[datasetDelimiter];
        }
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

    protected TIntSet getLocalCandidates(int referenceId, Geometry tEntity) {
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
                    candidateMatches.addAll(partialCandidates);
                }
            }
        }

        return candidateMatches;
    }

    @Override
    public String getMethodName() {
        return "Dynamic Progressive GIA.nt";
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
            int pairCounter = 0;
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

                        final TIntSet candidateMatches = localCandidates ? getLocalCandidates(counter, tEntity) : getCandidates(counter, tEntity);

                        final TIntIterator intIterator = candidateMatches.iterator();
                        while (intIterator.hasNext()) {
                            int candidateMatchId = intIterator.next();
                            if (validCandidate(candidateMatchId, tEntity.getEnvelopeInternal(), null)) {
                                pairCounter++;
                                float weight = getWeight(candidateMatchId, tEntity, wScheme);
                                if (minimumWeight <= weight) {
                                    final SortPair sp = new SortPair(pairCounter, candidateMatchId, counter, weight, tEntity);
                                    topKPairs.add(sp);
                                    if (budget < topKPairs.size()) {
                                        SortPair lastPair = topKPairs.pollFirst();
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

        for (SortPair sp : topKPairs) {
            List<SortPair> candidates = sourceCandidates.get(sp.getEntityId1());
            if (candidates == null) {
                candidates = new ArrayList<>();
                sourceCandidates.put(sp.getEntityId1(), candidates);
            }
            candidates.add(sp);

            candidates = targetCandidates.get(sp.getEntityId2());
            if (candidates == null) {
                candidates = new ArrayList<>();
                targetCandidates.put(sp.getEntityId2(), candidates);
            }
            candidates.add(sp);
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
        int updates = 0;
        while (!topKPairs.isEmpty()) {
            final SortPair sp = topKPairs.pollLast();

            boolean isRelated = relations.verifyRelations(sp.getEntityId1(), sp.getEntityId2(), sourceData[sp.getEntityId1()], sp.getTargetGeometry());
            if (isRelated) {
                List<SortPair> candidates = sourceCandidates.get(sp.getEntityId1());
                for (SortPair c : candidates) {
                    boolean exists = topKPairs.remove(c);
                    if (exists) {
                        updates++;
                        c.incrementRelatedMatches();
                        topKPairs.add(c);
                    }
                }

                candidates = targetCandidates.get(sp.getEntityId2());
                for (SortPair c : candidates) {
                    boolean exists = topKPairs.remove(c);
                    if (exists) {
                        updates++;
                        c.incrementRelatedMatches();
                        topKPairs.add(c);
                    }
                }
            }
        }
        System.out.println("No of updates\t:\t" + updates);
    }
}
