package batchAlgorithms;

import datamodel.RelatedGeometries;
import java.io.IOException;
import org.locationtech.jts.geom.Geometry;
import utilities.CsvReader;

public abstract class AbstractAlgorithm {

    protected int datasetDelimiter;

    protected double thetaX;
    protected double thetaY;

    protected long indexingTime;
    protected long verificationTime;

    protected Geometry[] sourceData;
    protected RelatedGeometries relations;
    protected final String delimiter;
    protected String targetFilePath;

    public AbstractAlgorithm(int qPairs, String delimiter, String sourceFilePath, String targetFilePath) throws IOException {
        sourceData = CsvReader.readAllEntities(delimiter, sourceFilePath);

        this.delimiter = delimiter;
        this.targetFilePath = targetFilePath;

        relations = new RelatedGeometries(qPairs);
        datasetDelimiter = sourceData.length;
    }

    public void applyProcessing() {
        setThetas();
    }

    public void printResults() {
        System.out.println("Indexing time\t:\t" + indexingTime);
        System.out.println("Verification time\t:\t" + verificationTime);
        relations.print();
    }

    protected abstract void setThetas();
}
