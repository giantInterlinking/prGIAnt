package experiments;

import batchAlgorithms.AbstractAlgorithm;
import datamodel.LightIndex;
import gnu.trove.list.TIntList;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TDoubleHashSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import utilities.CsvReader;

public class Table4 {

    static class TileStatistics extends AbstractAlgorithm {

        public TileStatistics(String delimiter, String sourceFilePath, String targetFilePath) throws IOException {
            super(0, delimiter, sourceFilePath, targetFilePath);
        }

        @Override
        public void applyProcessing() {
            super.applyProcessing();
            System.out.println("Max number of tiles\t:\t" + setApproximateNoOfBlocks());
            final LightIndex spatialIndex = indexSource();
            try {
                matchTargetData(spatialIndex);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void addToIndex(int geometryId, Envelope envelope, LightIndex spatialIndex) {
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

        private LightIndex indexSource() {
            final LightIndex spatialIndex = new LightIndex();

            int geometryId = 0;
            for (Geometry sEntity : sourceData) {
                final Envelope envelope = sEntity.getEnvelopeInternal();
                addToIndex(geometryId, envelope, spatialIndex);
                geometryId++;
            }

            return spatialIndex;
        }

        private void matchTargetData(LightIndex spatialIndex) throws ParseException, FileNotFoundException, IOException {
            int totalComparisons = 0;
            int uniqueComparisons = 0;
            final BufferedReader reader = new BufferedReader(new FileReader(targetFilePath));
            String line = reader.readLine();

            final TDoubleSet distinctTiles = new TDoubleHashSet();
            while (line != null) {
                String[] tokens = line.split(delimiter);
                Geometry tEntity = null;
                try {
                    if (2 <= tokens.length) {
                        tEntity = CsvReader.WKT_READER.read(tokens[1].trim());
                        if (tEntity == null) {
                            continue;
                        }
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

                                    double a = latIndex;
                                    double b = longIndex;
                                    double cantor = (a + b) * (a + b + 1) / 2 + a;
                                    distinctTiles.add(cantor);
                                    totalComparisons += partialCandidates.size();
                                }
                            }
                        }

                        uniqueComparisons += candidateMatches.size();
                    }
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }

                line = reader.readLine();
            }
            reader.close();
            System.out.println("Number of tiles\t:\t" + distinctTiles.size());
            System.out.println("Total pairs\t:\t" + totalComparisons);
            System.out.println("Unique pairs\t:\t" + uniqueComparisons);
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

        protected double setApproximateNoOfBlocks() {
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

            System.out.println("X-tiles\t:\t" + (globalMaxX - globalMinX + 1.0));
            System.out.println("Y-tiles\t:\t" + (globalMaxY - globalMinY + 1.0));

            return (globalMaxX - globalMinX + 1.0) * (globalMaxY - globalMinY + 1.0);
        }
    }

    public static void main(String[] args) throws ParseException, IOException {
        String mainDir = "/home/pyravlos/data/geometries/";

        String delimiter = "\"";
        String[] dataset1 = {"AREAWATER.csv", "AREAWATER.csv", "ROADS.csv"};
        String[] dataset2 = {"LINEARWATER.csv", "ROADS.csv", "EDGES.csv"};

        for (int i = 0; i < dataset1.length; i++) {
            TileStatistics giant = new TileStatistics(delimiter, mainDir + dataset1[i], mainDir + dataset2[i]);
            giant.applyProcessing();
            giant.printResults();
        }
    }
}
