package utilities;

import datamodel.Tile;
import java.util.Comparator;


public class IncTileCardinalityComparator implements Comparator<Tile> {

    /* 
    * This comparator orders tiles in increasing order of cardinality, i.e.,
    * from the smallest number of pairs to the largest one.
     */
    
    @Override
    public int compare(Tile block1, Tile block2) {
        return Double.compare(block1.getNoOfPairs(), block2.getNoOfPairs());
    }
}
