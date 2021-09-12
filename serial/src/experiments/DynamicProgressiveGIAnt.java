/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.io.ParseException;
import progressiveAlgorithms.DynamicProgressiveGIAnt;
import utilities.WeightingScheme;

public class DynamicProgressiveGIAnt {

    private final static int[] budgets = {5000000, 10000000};
    private final static int[][] qualifyingPairs = {{2401396, 199122, 3841922, 5000000, 5000000},
    {2401396, 199122, 3841922, 10000000, 10000000}};

    public static void main(String[] args) throws ParseException, IOException {
        String mainDir = "/home/gpapadakis/data/geometries/";

        String[] delimiter = {"\"", "\"", "\t", "\t", "\""};
        String[] dataset1 = {"AREAWATER.csv", "AREAWATER.csv", "lakes", "parks", "ROADS.csv"};
        String[] dataset2 = {"LINEARWATER.csv", "ROADS.csv", "parks", "roads", "EDGES.csv"};

        for (int i = 0; i < dataset1.length; i++) {
            for (int b = 0; b < budgets.length; b++) {
                for (WeightingScheme wScheme : WeightingScheme.values()) {
                    System.out.println("\n\nBudget\t:\t" + budgets[b]);
                    System.out.println("Dataset\t:\t" + dataset1[i] + "," + dataset2[i]);
                    System.out.println("Weighting scheme\t:\t" + wScheme);

                    DynamicProgressiveGIAnt alg = new DynamicProgressiveGIAnt(budgets[b], qualifyingPairs[b][i], delimiter[i], mainDir + dataset1[i], mainDir + dataset2[i], wScheme);
                    alg.applyProcessing();
                    alg.printResults();
                }
            }
        }
    }
}
