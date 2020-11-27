package experiments;

import java.io.IOException;
import org.locationtech.jts.io.ParseException;
import progressiveAlgorithms.AbstractProgressiveAlgorithm;
import utilities.WeightingScheme;

public class ProgressiveRADON {

    public static void main(String[] args) throws ParseException, IOException {
        int[] budgets = {5000000, 10000000};
        int[][] qPairs = {{2401396, 199122},
        {2401396, 199122}
        };
        String mainDir = "/data/geometries/";
        String delimiter = "\"";
        String[] dataset1 = {"AREAWATER.csv", "AREAWATER.csv"};
        String[] dataset2 = {"LINEARWATER.csv", "ROADS.csv"};

        for (int b = 0; b < budgets.length; b++) {
            for (int tileOrdering = 0; tileOrdering < 2; tileOrdering++) {
                for (int i = 1; i < dataset1.length; i++) {
                    for (WeightingScheme wScheme : WeightingScheme.values()) {
                        System.out.println("\n\nBudget\t:\t" + budgets[b]);
                        System.out.println("Tile ordering\t:\t" + tileOrdering);
                        System.out.println("Dataset\t:\t" + dataset1[i] + "," + dataset2[i]);
                        System.out.println("Weighting scheme\t:\t" + wScheme);

                        AbstractProgressiveAlgorithm alg = new progressiveAlgorithms.ProgressiveRADON(tileOrdering == 0, budgets[b], qPairs[b][i], delimiter, mainDir + dataset1[i], mainDir + dataset2[i], wScheme);
                        alg.applyProcessing();
                        alg.printResults();
                    }
                }
            }
        }
    }
}
