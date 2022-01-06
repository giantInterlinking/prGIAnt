package supervisedFiltering;

import datamodel.LightIndex;
import datamodel.RelatedGeometries;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import utilities.CsvReader;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author gap2
 */
public class ApproximateGI {

    private final static int CLASS_SIZE = 500;
    private final static int NO_OF_FEATURES = 6;
    private final static int SAMPLE_SIZE = 20000;

    private final static int POSITIVE_PAIR = 1;
    private final static int NEGATIVE_PAIR = 0;

    private final static String RELATED = "related";
    private final static String NON_RELATED = "nonrelated";

    protected long indexingTime;
    protected long initializationTime;
    protected long trainingTime;
    protected long verificationTime;

    protected double thetaX;
    protected double thetaY;

    protected double maxDistinctSourceOccurrences;
    protected double maxDistinctTargetOccurrences;
    protected double maxSourceCandidates;
    protected double maxSourceOccurrences;
    protected double maxTargetCandidates;
    protected double maxTargetOccurrences;

    protected int datasetDelimiter;
    protected int[] distinctCooccurrences;
    protected int[] flags;
    protected int[] frequency;
    protected int[] realCandidates;
    protected int[] totalCooccurrences;

    protected Attribute classAttribute;
    protected ArrayList<Attribute> attributes;
    protected Classifier classifier;
    protected Geometry[] sourceData;
    protected Instances trainingInstances;
    protected LightIndex spatialIndex;
    protected List<String> classLabels;
    protected List<SamplePair> sample;
    protected final RelatedGeometries relations;
    protected Set<VerifiedPair> verifiedPairs;
    protected final String delimiter;
    protected final String sourceFilePath;
    protected final String targetFilePath;
    protected static final WKTReader WKT_READER = new WKTReader();

    public ApproximateGI(int qPairs, String delimiter, String sourceFilePath, String targetFilePath) throws IOException {
        this.delimiter = delimiter;

        sourceData = CsvReader.readAllEntities(delimiter, sourceFilePath);

        this.sourceFilePath = sourceFilePath;
        this.targetFilePath = targetFilePath;

        datasetDelimiter = sourceData.length;
        relations = new RelatedGeometries(qPairs);

        sample = new ArrayList<>();

        getAttributes();
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

    public void applyProcessing() throws Exception {
        long time1 = System.currentTimeMillis();
        setThetas();
        indexSource();
        long time2 = System.currentTimeMillis();
        preprocessing();
        long time3 = System.currentTimeMillis();
        trainModel();
        long time4 = System.currentTimeMillis();
        verification();
        long time5 = System.currentTimeMillis();
        indexingTime = time2 - time1;
        initializationTime = time3 - time2;
        trainingTime = time4 - time3;
        verificationTime = time5 - time4;

        System.out.println("Indexing Time\t:\t" + indexingTime);
        System.out.println("Initialization Time\t:\t" + initializationTime);
        System.out.println("Training Time\t:\t" + trainingTime);
        System.out.println("Verification Time\t:\t" + verificationTime);
        relations.print();
    }

    private void getAttributes() {
        attributes = new ArrayList<>();
        for (int i = 0; i < NO_OF_FEATURES; i++) {
            attributes.add(new Attribute("FEATURE" + i));
        }

        classLabels = new ArrayList<>();
        classLabels.add(NON_RELATED);
        classLabels.add(RELATED);

        classAttribute = new Attribute("class", classLabels);
        attributes.add(classAttribute);
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
                        if (flags[currentId] != referenceId) {
                            flags[currentId] = referenceId;
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

    protected TIntSet getFeatureCandidates(int referenceId, Geometry tEntity) {
        frequency = new int[datasetDelimiter];
        return getCandidates(referenceId, tEntity);
    }

    protected Instance getFeatureVector(int classLabel, int sourceId, int targetId, Geometry target) {
        double[] featureVector = new double[NO_OF_FEATURES + 1];

        final TIntSet candidateMatches = getFeatureCandidates(targetId, target);
        final TIntIterator intIterator = candidateMatches.iterator();
        while (intIterator.hasNext()) {
            int candidateMatchId = intIterator.next();
            featureVector[3] += frequency[candidateMatchId];
            featureVector[4]++;
            if (validCandidate(candidateMatchId, target.getEnvelopeInternal())) { // intersecting MBRs
                featureVector[5]++;
            }
        }

        //candidate-based features
        //raw features
        //source geometry
        featureVector[0] = totalCooccurrences[sourceId] / maxSourceOccurrences * 10000;
        featureVector[1] = distinctCooccurrences[sourceId] / maxDistinctSourceOccurrences * 10000;
        featureVector[2] = realCandidates[sourceId] / maxSourceCandidates * 10000;
        //target geometry
        featureVector[3] = featureVector[3] / maxTargetOccurrences * 10000;
        featureVector[4] = featureVector[4] / maxDistinctTargetOccurrences * 10000;
        featureVector[5] = featureVector[5] / maxTargetCandidates * 10000;

        featureVector[6] = classLabel; // 0 for negative, 1 for positive instances

        Instance newInstance = new DenseInstance(1.0, featureVector);
        newInstance.setDataset(trainingInstances);

        return newInstance;
    }

    public String getMethodName() {
        return "Approximate Geospatial Interlinking";
    }

    protected LightIndex indexSource() {
        spatialIndex = new LightIndex();

        int geometryId = 0;
        for (Geometry sEntity : sourceData) {
            final Envelope envelope = sEntity.getEnvelopeInternal();
            addToIndex(geometryId, envelope, spatialIndex);
            geometryId++;
        }

        return spatialIndex;
    }

    //get sample of unlabelled instances and features for source datasets
    protected void preprocessing() {
        flags = new int[datasetDelimiter];
        frequency = new int[datasetDelimiter];
        distinctCooccurrences = new int[datasetDelimiter];
        realCandidates = new int[datasetDelimiter];
        totalCooccurrences = new int[datasetDelimiter];

        int maxCandidatePairs = 10 * datasetDelimiter;
        final Random random = new Random();
        final Set<Integer> pairIds = new HashSet<>();
        while (pairIds.size() < SAMPLE_SIZE) {
            pairIds.add(random.nextInt(maxCandidatePairs));
        }

        maxDistinctSourceOccurrences = 0;
        maxDistinctTargetOccurrences = 0;
        maxSourceCandidates = 0;
        maxSourceOccurrences = 0;
        maxTargetCandidates = 0;
        maxTargetOccurrences = 0;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(targetFilePath));
            String line = reader.readLine();
            int counter = 0;
            int pairId = 0;
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
                            line = reader.readLine();
                            continue;
                        }

                        final TIntSet candidateMatches = getCandidates(counter, tEntity);

                        int currentCandidates = 0;
                        int currentDistinctCooccurrences = candidateMatches.size();
                        int currentCooccurrences = 0;

                        final TIntIterator intIterator = candidateMatches.iterator();
                        while (intIterator.hasNext()) {
                            int candidateMatchId = intIterator.next();
                            distinctCooccurrences[candidateMatchId]++;
                            currentCooccurrences += frequency[candidateMatchId];
                            totalCooccurrences[candidateMatchId] += frequency[candidateMatchId];
                            if (validCandidate(candidateMatchId, tEntity.getEnvelopeInternal())) { // intersecting MBRs
                                currentCandidates++;
                                realCandidates[candidateMatchId]++;

                                if (pairIds.contains(pairId)) {
                                    sample.add(new SamplePair(candidateMatchId, counter, tEntity));
                                }
                                pairId++;
                            }
                        }

                        if (maxDistinctTargetOccurrences < currentDistinctCooccurrences) {
                            maxDistinctTargetOccurrences = currentDistinctCooccurrences;
                        }
                        if (maxTargetOccurrences < currentCooccurrences) {
                            maxTargetOccurrences = currentCooccurrences;
                        }
                        if (maxTargetCandidates < currentCandidates) {
                            maxTargetCandidates = currentCandidates;
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

        for (int i = 0; i < datasetDelimiter; i++) {
            if (maxDistinctSourceOccurrences < distinctCooccurrences[i]) {
                maxDistinctSourceOccurrences = distinctCooccurrences[i];
            }
            if (maxSourceOccurrences < totalCooccurrences[i]) {
                maxSourceOccurrences = totalCooccurrences[i];
            }
            if (maxSourceCandidates < realCandidates[i]) {
                maxSourceCandidates = realCandidates[i];
            }
        }

//        System.out.println("maxDistinctSourceOccurrences=" + maxDistinctSourceOccurrences);
//        System.out.println("maxDistinctTargetOccurrences=" + maxDistinctTargetOccurrences);
//        System.out.println("maxSourceCandidates=" + maxSourceCandidates);
//        System.out.println("maxSourceOccurrences=" + maxSourceOccurrences);
//        System.out.println("maxTargetCandidates=" + maxTargetCandidates);
//        System.out.println("maxTargetOccurrences=" + maxTargetOccurrences);
    }

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
    }

    protected void trainModel() throws Exception {
        int excessVerifications = 0;
        verifiedPairs = new HashSet<>();

        boolean negativeClassFull = false;
        boolean positiveClassFull = false;
        final List<SamplePair> negativePairs = new ArrayList<>();
        final List<SamplePair> positivePairs = new ArrayList<>();
        Collections.shuffle(sample);
        for (SamplePair p : sample) {
            if (negativeClassFull && positiveClassFull) {
                break;
            }

            boolean isRelated = relations.verifyRelations(p.getSourceId(), p.getTargetId(), sourceData[p.getSourceId()], p.getGeometry());
            verifiedPairs.add(new VerifiedPair(p.getSourceId(), p.getTargetId()));

            if (isRelated) {
                if (positivePairs.size() < CLASS_SIZE) {
                    positivePairs.add(p);
                } else {
                    excessVerifications++;
                    positiveClassFull = true;
                }
            } else {
                if (negativePairs.size() < CLASS_SIZE) {
                    negativePairs.add(p);
                } else {
                    excessVerifications++;
                    negativeClassFull = true;
                }
            }
        }

        System.out.println("Excess verifications\t:\t" + excessVerifications);
        System.out.println("Labelled negative instances\t:\t" + negativePairs.size());
        System.out.println("Labelled positive instances\t:\t" + positivePairs.size());

        trainingInstances = new Instances("trainingSet", attributes, 2 * positivePairs.size());
        trainingInstances.setClassIndex(NO_OF_FEATURES);
        for (int i = 0; i < positivePairs.size(); i++) {
            SamplePair negPair = negativePairs.get(i);
            trainingInstances.add(getFeatureVector(NEGATIVE_PAIR, negPair.getSourceId(), negPair.getTargetId(), negPair.getGeometry()));

            SamplePair posPair = positivePairs.get(i);
            trainingInstances.add(getFeatureVector(POSITIVE_PAIR, posPair.getSourceId(), posPair.getTargetId(), posPair.getGeometry()));
        }

        classifier = new Logistic();
        classifier.buildClassifier(trainingInstances);
//        System.out.println(classifier.toString());
    }

    protected boolean validCandidate(int candidateId, Envelope e2) {
        return sourceData[candidateId].getEnvelopeInternal().intersects(e2);
    }

    protected void verification() throws Exception {
        int positiveDecisions = 0;
        int totalDecisions = 0;
        flags = new int[datasetDelimiter];
        frequency = new int[datasetDelimiter];

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
                            line = reader.readLine();
                            continue;
                        }

                        final TIntSet candidateMatches = getCandidates(counter, tEntity);

                        int realPairs = 0;
                        int distinctPairs = candidateMatches.size();
                        int totalPairs = 0;
                        TIntIterator intIterator = candidateMatches.iterator();
                        while (intIterator.hasNext()) {
                            int candidateMatchId = intIterator.next();
                            totalPairs += frequency[candidateMatchId];
                            if (validCandidate(candidateMatchId, tEntity.getEnvelopeInternal())) {
                                realPairs++;
                            }
                        }

                        intIterator = candidateMatches.iterator();
                        while (intIterator.hasNext()) {
                            int candidateMatchId = intIterator.next();

                            if (validCandidate(candidateMatchId, tEntity.getEnvelopeInternal())) {
                                if (verifiedPairs.contains(new VerifiedPair(candidateMatchId, counter))) {
                                    continue;
                                }

                                totalDecisions++;

                                double[] featureVector = new double[NO_OF_FEATURES + 1];
                                //candidate-based features
                                //raw features
                                //source geometry
                                featureVector[0] = totalCooccurrences[candidateMatchId] / maxSourceOccurrences * 10000;
                                featureVector[1] = distinctCooccurrences[candidateMatchId] / maxDistinctSourceOccurrences * 10000;
                                featureVector[2] = realCandidates[candidateMatchId] / maxSourceCandidates * 10000;
                                //target geometry
                                featureVector[3] = totalPairs / maxTargetOccurrences * 10000;
                                featureVector[4] = distinctPairs / maxDistinctTargetOccurrences * 10000;
                                featureVector[5] = realPairs / maxTargetCandidates * 10000;

                                Instance currentInstance = new DenseInstance(1.0, featureVector);
                                currentInstance.setDataset(trainingInstances);

                                int instanceLabel = (int) classifier.classifyInstance(currentInstance);
                                if (instanceLabel == POSITIVE_PAIR) {
                                    positiveDecisions++;
                                    relations.verifyRelations(candidateMatchId, counter, sourceData[candidateMatchId], tEntity);
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
        System.out.println("Positive Decisions\t:\t" + positiveDecisions);
        System.out.println("Total Decisions\t:\t" + totalDecisions);
    }
}
