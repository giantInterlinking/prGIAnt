package experiments;

import batchAlgorithms.GIAnt;
import java.io.IOException;
import org.locationtech.jts.io.ParseException;

public class BatchGIAnt {

    public static void main(String[] args) throws ParseException, IOException {
        String mainDir = "/data/geometries/";
        String delimiter = "\"";
        String[] dataset1 = {"AREAWATER.csv", "AREAWATER.csv", "ROADS.csv"};
        String[] dataset2 = {"LINEARWATER.csv", "ROADS.csv", "EDGES.csv"};

        int[] qPairs = {2401396, 199122, 163982135};

        for (int i = 0; i < dataset1.length; i++) {
            GIAnt giant = new GIAnt(qPairs[i], delimiter, mainDir + dataset1[i], mainDir + dataset2[i]);
            giant.applyProcessing();
            giant.printResults();
        }
    }
}
