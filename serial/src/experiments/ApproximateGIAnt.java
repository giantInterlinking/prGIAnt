package experiments;

import java.io.IOException;
import org.locationtech.jts.io.ParseException;
import supervisedFiltering.ApproximateGI;

/**
 *
 * @author Georgios
 */
public class ApproximateGIAnt {

    public static void main(String[] args) throws ParseException, IOException, Exception {
        String mainDir = "/home/gpapadakis/data/geometries/";
        String[] delimiter = {"\"", "\"", "\t", "\t", "\""};
        String[] dataset1 = {"AREAWATER.csv", "AREAWATER.csv", "lakes", "parks", "ROADS.csv"};
        String[] dataset2 = {"LINEARWATER.csv", "ROADS.csv", "parks", "roads", "EDGES.csv"};

        int[] qPairs = {2401396, 199122, 3841922, 5000000, 163982138};
        for (int i = 4; i < dataset1.length; i++) {
            for (int j = 0; j < 5; j++) {
                ApproximateGI agi = new ApproximateGI(qPairs[i], delimiter[i], mainDir + dataset1[i], mainDir + dataset2[i]);
                agi.applyProcessing();
            }
        }
    }
}
