package utilities;

import datamodel.Pair;
import java.util.Comparator;

public class DecPairWeightComparator implements Comparator<Pair> {

    /* 
    * This comparator orders pairs in decreasing order of weight,
    * i.e., from the largest weight to the smallest one.
    */
    
    @Override
    public int compare(Pair p1, Pair p2) {
        float test = p1.getWeight()- p2.getWeight(); 
        if (0 < test) {
            return -1;
        }

        if (test < 0) {
            return 1;
        }

        return 0;
    }
}